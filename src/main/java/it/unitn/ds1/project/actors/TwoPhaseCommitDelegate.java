package it.unitn.ds1.project.actors;

import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class TwoPhaseCommitDelegate {

    private final ReplicaActor replicaActor;

    private final Map<Timestamp, Integer> updatesWaitingForOk = new HashMap<>();

    /**
     * used only by the master
     */
    private final Map<Timestamp, Integer> acksCount = new HashMap<>();

    /**
     * used only by the master
     */
    private Timestamp latestTimestamp = new Timestamp(0, 0);

    public TwoPhaseCommitDelegate(ReplicaActor replicaActor) {
        this.replicaActor = replicaActor;
    }

    void onClientUpdateMsg(ClientUpdate msg) {
        ReplicaUpdate forwardedMessage = ReplicaUpdate.fromClientUpdate(msg);
        replicaActor.tellMaster(forwardedMessage);
        replicaActor.getTimeoutManager().startTimeout(forwardedMessage, 1000, new MasterTimeout()); // TODO: replace with constant
    }

    void onReplicaUpdateMsg(ReplicaUpdate msg) {
        if (!replicaActor.amIMaster()) {
            replicaActor.logMessageIgnored("non-master replica shouldn't receive messages of type ReplicaUpdate");
            return;
        }
        latestTimestamp = latestTimestamp.nextUpdate();
        acksCount.put(latestTimestamp, 0);
        replicaActor.tellBroadcast(MasterUpdate.fromReplicaUpdate(msg, latestTimestamp));
    }

    void onMasterUpdateMsg(MasterUpdate msg) {
        replicaActor.getTimeoutManager().cancelTimeout(msg); // will cancel the timeout only if received by the replica that request update
        updatesWaitingForOk.put(msg.timestamp, msg.value);
        ReplicaUpdateAck ackForMaster = ReplicaUpdateAck.fromMasterUpdate(msg);
        replicaActor.tellMaster(ackForMaster);
        replicaActor.getTimeoutManager().startTimeout(ackForMaster, 1000, new MasterTimeout()); // TODO: Replace with constant
    }

    void onReplicaUpdateAckMsg(ReplicaUpdateAck msg) {
        if (!replicaActor.amIMaster()) {
            replicaActor.logMessageIgnored("non-master replica shouldn't receive messages of type ReplicaUpdateAck");
            return;
        }
        Timestamp timestamp = msg.timestamp;
        int updatedCount = acksCount.merge(timestamp, 1, Integer::sum);
        if (updatedCount >= getQuorum()) {
            replicaActor.log("quorum for message " + timestamp + " reached");
            acksCount.remove(timestamp);
            replicaActor.tellBroadcast(new MasterUpdateOk(timestamp));
        }
    }

    void onMasterUpdateOkMsg(MasterUpdateOk msg) {
        replicaActor.getTimeoutManager().cancelTimeout(msg);
        if (!updatesWaitingForOk.containsKey(msg.timestamp)) {
            replicaActor.logMessageIgnored("unknown update with timestamp " + msg.timestamp);
            return;
        }
        int updatedValue = updatesWaitingForOk.remove(msg.timestamp);
        replicaActor.setValue(updatedValue);
        replicaActor.getUpdateHistory().add(msg.timestamp);
        replicaActor.log("update " + msg.timestamp + " " + updatedValue);
    }

    private int getQuorum() {
        return Math.floorDiv(replicaActor.getReplicas().size(), 2) + 1;
    }
}
