package it.unitn.ds1.project.delegates;

import it.unitn.ds1.project.models.Messages.*;
import it.unitn.ds1.project.managers.TimeoutManager;
import it.unitn.ds1.project.models.Timestamp;
import it.unitn.ds1.project.actors.ReplicaActor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static it.unitn.ds1.project.managers.Timeouts.ELECTION_ACK_TIMEOUT_MS;

public class ElectionDelegate {
    private final TimeoutManager timeoutManager;
    private final ReplicaActor replica;

    private int next;

    public ElectionDelegate(ReplicaActor replica) {
        this.replica = replica;
        timeoutManager = replica.getTimeoutManager();
    }

    public void onMasterTimeoutMsg(MasterTimeout msg) {
        replica.log("i've noticed master is dead. I start a new election");
        startElection(new ReplicaElection());
    }

    public void startElection(ReplicaElection partial) {
        Map<Integer, Timestamp> updatedHistory = new HashMap<>(partial.latestUpdatesByNodeId);
        updatedHistory.put(replica.getId(), replica.getLatestUpdate().timestamp);

        Map<Timestamp, Integer> updatedPartial = new HashMap<>(partial.updatesWaitingForOk);
        replica.getUpdatesWaitingForOk().forEach(updatedPartial::putIfAbsent);
        ReplicaElection updated = new ReplicaElection(updatedHistory, updatedPartial);

        tellNext(updated);
        timeoutManager.cancelAllExceptMasterHeartBeat();
        ReplicaNextDead rnd = new ReplicaNextDead(partial, next);
        timeoutManager.startTimeout(updated, ELECTION_ACK_TIMEOUT_MS, rnd);
    }

    public void onReplicaElectionAckMsg(ReplicaElectionAck msg) {
        timeoutManager.cancelTimeout(msg);
    }

    public void onReplicaElectionMsg(ReplicaElection msg) {
        replica.log(msg.toString());
        replica.getSender().tell(new ReplicaElectionAck(msg.id), replica.getSelf());
        if (replica.isMaster()) {
            // I'm the master and I'm still alive, no need to continue election
        } else if (msg.latestUpdatesByNodeId.containsKey(replica.getId())) { //full ring trip done
            this.pickMaster(msg);
        } else {
            this.startElection(msg);
        }
    }

    public void pickMaster(ReplicaElection electionMsg) {
        int newMaster = findMostUpdatedNode(electionMsg);
        if (replica.getId() == newMaster && !replica.isMaster()) {

            Timestamp newTimestamp = replica.getLatestUpdate().timestamp.nextEpoch();
            replica.setMasterTimestamp(newTimestamp);
            replica.setMasterId(newMaster);
            replica.tellBroadcast(new MasterSync(newTimestamp, newMaster, replica.getHistory()));
            replica.log("i'm the new master");

            if(!electionMsg.updatesWaitingForOk.isEmpty()) {
                Timestamp latestPendingUpdateTimestamp = Collections.max(electionMsg.updatesWaitingForOk.keySet());
                replica.self().tell(new ReplicaUpdate(electionMsg.updatesWaitingForOk.get(latestPendingUpdateTimestamp)), replica.self());
            }
        } else if (!replica.isMaster()) {
            replica.getReplicas().get(newMaster).tell(electionMsg, replica.getSelf());
        }
    }

    public int findMostUpdatedNode(ReplicaElection electionMsg) {
        Optional<Map.Entry<Integer, Timestamp>> mostUpdatedNode = electionMsg.latestUpdatesByNodeId
                .entrySet()
                .stream()
                .max((a, b) -> {
                    int historyComparison = Timestamp.COMPARATOR.compare(a.getValue(), b.getValue());
                    if (historyComparison == 0) {
                        return b.getKey() - a.getKey(); // we prefer actor lowest id
                    } else {
                        return historyComparison;
                    }
                });
        if (mostUpdatedNode.isPresent()) {
            return mostUpdatedNode.get().getKey();
        } else {
            // This should happen only at the beginning of the protocol, when there is no master and no updates
            return 0;
        }
    }

    public void onReplicaNextDead(ReplicaNextDead msg) {
        boolean haveToBump = msg.next == next;
        String part = "";
        for (Integer i : msg.partial.latestUpdatesByNodeId.keySet()) {
            part += i;
        }
        replica.log("next (" + next + ") is dead, partial is: " +
                part + "and I will " + (haveToBump ? "" : "not") + "bump");
        if (haveToBump) {
            bumpNext();
        }
        startElection(msg.partial);
    }

    public void onMasterSyncMsg(MasterSync msg) {
        replica.setHistory(msg.history);
        replica.setMasterId(msg.masterId);
        replica.endElection();
    }


    private void bumpNext() {
        next = ((next + 1) % replica.getReplicas().size());
        if (replica.getId() == next) {
            this.bumpNext();
        }
    }

    private void tellNext(Object msg) {
        replica.getReplicas().get(next).tell(msg, replica.getSelf());
    }

    public void onStartMsg(Start msg) {
        next = replica.getId();
        bumpNext();
    }
}
