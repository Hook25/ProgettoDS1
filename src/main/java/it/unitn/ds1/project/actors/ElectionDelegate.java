package it.unitn.ds1.project.actors;

import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.TimeoutManager;
import it.unitn.ds1.project.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ElectionDelegate {

    private static final int ELECTION_ACK_TIMEOUT_MS = 400;

    private final TimeoutManager timeoutManager;
    private final ReplicaActor replica;

    private int next;

    public ElectionDelegate(ReplicaActor replica) {
        this.replica = replica;
        timeoutManager = replica.getTimeoutManager();
    }

    void onMasterTimeoutMsg(MasterTimeout msg) {
        replica.log("i've noticed master is dead. I start a new election");
        startElection(new HashMap<>());
    }

    void startElection(Map<Integer, Timestamp> partial) {
        partial = new HashMap<>(partial);
        partial.put(replica.getId(), replica.getLatestUpdate().timestamp);
        ReplicaElection toSend = new ReplicaElection(partial);
        tellNext(toSend);
        timeoutManager.startTimeout(toSend, ELECTION_ACK_TIMEOUT_MS, new ReplicaNextDead(partial));
    }

    void onReplicaElectionAckMsg(ReplicaElectionAck msg) {
        timeoutManager.cancelTimeout(msg);
    }

    void onReplicaElectionMsg(ReplicaElection msg) {
        replica.log("i've received an election");
        replica.cancelHeartbeat();
        replica.getSender().tell(new ReplicaElectionAck(msg.id), replica.getSelf());
        if (msg.latestUpdatesByNodeId.containsKey(replica.getId())) { //full ring trip done
            this.pickMaster(msg.latestUpdatesByNodeId);
        } else {
            this.startElection(msg.latestUpdatesByNodeId);
        }
    }

    void pickMaster(Map<Integer, Timestamp> latestUpdatesByNodeId) {
        int newMaster = findMostUpdatedNode(latestUpdatesByNodeId);
        if (replica.getId() == newMaster) {
            /*
             *  TODO: is this the right way to update the timestamp?
             *  MasterSync should include  anew timestamp (next epoch) or the timestamp of the latest timestamp?
             *  Moreover, are we completing pending updates during election?
             */
            Timestamp newTimestamp = replica.getLatestUpdate().timestamp.nextEpoch();
            replica.setMasterTimestamp(newTimestamp);
            replica.tellBroadcast(new MasterSync(newTimestamp, newMaster, replica.getHistory()));
            replica.log("i'm the new master");
        }
    }

    int findMostUpdatedNode(Map<Integer, Timestamp> latestUpdatesByNodeId) {
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
            return 0;
        }
    }

    void onReplicaNextDead(ReplicaNextDead msg) {
        replica.log("next (" + next + ") is dead");
        bumpNext();
        startElection(msg.partial);
    }

    void onMasterSyncMsg(MasterSync msg) {
        replica.setHistory(msg.history);
        replica.setMasterId(msg.masterId);
        replica.endElection(); // TODO: should we cancel previous heartbeat timeout?
    }


    private void bumpNext() {
        // TODO: this doesn't work if there is only a replica. Should we fix it?
        next = ((next + 1) % replica.getReplicas().size());
        if (replica.getId() == next) {
            this.bumpNext();
        }
    }

    private void tellNext(Object msg) {
        replica.getReplicas().get(next).tell(msg, replica.self());
    }

    public void onStartMsg(Start msg) {
        next = replica.getId();
        bumpNext();
    }
}
