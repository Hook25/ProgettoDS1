package it.unitn.ds1.project.actors;

import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class TwoPhaseCommitDelegate {

    private static final int MASTER_UPDATE_TIMEOUT = 400;

    private static final int MASTER_UPDATE_OK_TIMEOUT = 400;

    private final ReplicaActor replicaActor;

    private final Map<Timestamp, Integer> updatesWaitingForOk = new HashMap<>();

    /**
     * used only by the master
     */
    private final Map<Timestamp, Integer> acksCount = new HashMap<>();

    /**
     * used only by the master
     */
    private Timestamp masterTimestamp = new Timestamp(0, 0);

    public TwoPhaseCommitDelegate(ReplicaActor replicaActor) {
        this.replicaActor = replicaActor;
    }

    void onClientUpdateMsg(ClientUpdate msg) {
        ReplicaUpdate forwardedMessage = ReplicaUpdate.fromClientUpdate(msg);
        replicaActor.tellMaster(forwardedMessage);
        replicaActor.getTimeoutManager().startTimeout(forwardedMessage, MASTER_UPDATE_TIMEOUT, new MasterTimeout());
    }

    void onReplicaUpdateMsg(ReplicaUpdate msg) {
        if (!replicaActor.isMaster()) {
            replicaActor.logMessageIgnored("non-master replica shouldn't receive messages of type ReplicaUpdate");
            return;
        }
        masterTimestamp = masterTimestamp.nextUpdate();
        replicaActor.setLatestUpdate(masterTimestamp);
        acksCount.put(masterTimestamp, 0);
        replicaActor.tellBroadcast(MasterUpdate.fromReplicaUpdate(msg, masterTimestamp));
    }

    void onMasterUpdateMsg(MasterUpdate msg) {
        replicaActor.getTimeoutManager().cancelTimeout(msg); // will cancel the timeout only if received by the replica that request update
        replicaActor.getHeartbeatDelegate().postponeHeartBeatTimeout();
        replicaActor.log("received " + msg);
        updatesWaitingForOk.put(msg.timestamp, msg.value);
        ReplicaUpdateAck ackForMaster = ReplicaUpdateAck.fromMasterUpdate(msg);
        replicaActor.tellMaster(ackForMaster);
        replicaActor.getTimeoutManager().startTimeout(ackForMaster, MASTER_UPDATE_OK_TIMEOUT, new MasterTimeout());
    }

    void onReplicaUpdateAckMsg(ReplicaUpdateAck msg) {
        if (!replicaActor.isMaster()) {
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
        replicaActor.getHeartbeatDelegate().postponeHeartBeatTimeout();
        replicaActor.log("received " + msg);
        if (!updatesWaitingForOk.containsKey(msg.timestamp)) {
            replicaActor.logMessageIgnored("unknown update with timestamp " + msg.timestamp);
            return;
        }
        int updatedValue = updatesWaitingForOk.remove(msg.timestamp);
        replicaActor.setValue(updatedValue);
        replicaActor.setLatestUpdate(msg.timestamp);
        replicaActor.log("update " + msg.timestamp + " " + updatedValue);
    }

    private int getQuorum() {
        return Math.floorDiv(replicaActor.getReplicas().size(), 2) + 1;
    }

    public void setMasterTimestamp(Timestamp timestamp) {
        masterTimestamp = timestamp;
    }
}
