package it.unitn.ds1.project.delegates;

import it.unitn.ds1.project.models.Messages.*;
import it.unitn.ds1.project.managers.TimeoutManager;
import it.unitn.ds1.project.models.Timestamp;
import it.unitn.ds1.project.actors.ReplicaActor;

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
        startElection(new HashMap<>());
    }

    public void startElection(Map<Integer, Timestamp> partial) {
        Map<Integer, Timestamp>   updated = new HashMap<>(partial);
        updated.put(replica.getId(), replica.getLatestUpdate().timestamp);
        ReplicaElection toSend = new ReplicaElection(updated);
        tellNext(toSend);
        ReplicaNextDead rnd = new ReplicaNextDead(partial, next);
        timeoutManager.startTimeout(toSend, ELECTION_ACK_TIMEOUT_MS, rnd);
    }

    public void onReplicaElectionAckMsg(ReplicaElectionAck msg) {
        timeoutManager.cancelTimeout(msg);
    }

    public void onReplicaElectionMsg(ReplicaElection msg) {
        String inner_msgs = "Election ->";
        for(Integer k : msg.latestUpdatesByNodeId.keySet()){
            inner_msgs += k;
        }
        replica.log(inner_msgs);
        replica.getSender().tell(new ReplicaElectionAck(msg.id), replica.getSelf());
        if (msg.latestUpdatesByNodeId.containsKey(replica.getId())) { //full ring trip done
            this.pickMaster(msg.latestUpdatesByNodeId);
        } else {
            this.startElection(msg.latestUpdatesByNodeId);
        }
    }

    public void pickMaster(Map<Integer, Timestamp> latestUpdatesByNodeId) {
        int newMaster = findMostUpdatedNode(latestUpdatesByNodeId);
        if (replica.getId() == newMaster) {
            /*
             *  TODO: is this the right way to update the timestamp?
             *  MasterSync should include a new timestamp (next epoch) or the timestamp of the latest timestamp?
             *  Moreover, are we completing pending updates during election?
             */
            Timestamp newTimestamp = replica.getLatestUpdate().timestamp.nextEpoch();
            replica.setMasterTimestamp(newTimestamp);
            replica.tellBroadcast(new MasterSync(newTimestamp, newMaster, replica.getHistory()));
            replica.log("i'm the new master");
        }
    }

    public int findMostUpdatedNode(Map<Integer, Timestamp> latestUpdatesByNodeId) {
        Optional<Map.Entry<Integer, Timestamp>> mostUpdatedNode = latestUpdatesByNodeId
                .entrySet()
                .stream()
                .max((a, b) -> {
                    int comparison = Timestamp.COMPARATOR.compare(a.getValue(), b.getValue());
                    if (comparison == 0) {
                        return b.getKey() - a.getKey(); // we prefer actor lowest id
                    } else {
                        return comparison;
                    }
                });
        if (mostUpdatedNode.isPresent()) {
            return mostUpdatedNode.get().getKey();
        } else {
            // TODO: What to do if no one is the best to become the new master?
            // This should happen only at the beginning of the protocol, when there is no master and no updates
            return 0;
        }
    }

    public void onReplicaNextDead(ReplicaNextDead msg) {
        boolean haveToBump = msg.next == next;
        String part = "";
        for(Integer i : msg.partial.keySet()){
            part += i;
        }
        replica.log("next (" + next + ") is dead, partial is: " +
                part + "and I will " + (haveToBump ? "" : "not") + "bump");
        if(haveToBump) {
            bumpNext();
        }
        startElection(msg.partial);
    }

    public void onMasterSyncMsg(MasterSync msg) {
        replica.setHistory(msg.history);
        replica.setMasterId(msg.masterId);
        replica.endElection(); // TODO: should we cancel previous heartbeat timeout?
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
