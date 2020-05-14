package it.unitn.ds1.project.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplicaActor extends ActorWithId {

    private int value;

    private List<ActorRef> replicas;

    private int masterId;

    private final Map<Timestamp, Integer> updatesWaitingForOk = new HashMap<>();

    private final List<Timestamp> updateHistory = new ArrayList<>();

    /**
     * used only by the master
     */
    private final Map<Timestamp, Integer> acksCount = new HashMap<>();

    /**
     * used only by the master
     */
    private Timestamp latestTimestamp = new Timestamp(0, 0);

    static public Props props(int id) {
        return Props.create(ReplicaActor.class, () -> new ReplicaActor(id));
    }

    public ReplicaActor(int id) {
        super("Replica", id);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Start.class, this::onStartMsg)
                .match(ClientUpdate.class, this::onClientUpdateMsg)
                .match(ReplicaUpdate.class, this::onReplicaUpdateMsg)
                .match(MasterUpdate.class, this::onMasterUpdateMsg)
                .match(ReplicaUpdateAck.class, this::onReplicaUpdateAckMsg)
                .match(MasterUpdateOk.class, this::onMasterUpdateOkMsg)
                .match(MasterHeartBeat.class, this::onMasterHeartBeatMsg)
                .match(ReplicaElection.class, this::onReplicaElectionMsg)
                .match(MasterSync.class, this::onMasterSyncMsg)
                .match(ReplicaElectionAck.class, this::onReplicaElectionAckMsg)
                .match(ClientRead.class, this::onClientReadMsg)
                .build();
    }

    private void onStartMsg(Start msg) {
        this.replicas = msg.replicas;
        this.value = msg.initialValue;
        this.masterId = msg.masterId;
    }

    private void onClientUpdateMsg(ClientUpdate msg) {
        ReplicaUpdate forwardedMessage = ReplicaUpdate.fromClientUpdate(msg);
        tellMaster(forwardedMessage);
    }

    private void onReplicaUpdateMsg(ReplicaUpdate msg) {
        if (!amIMaster()) {
            logMessageIgnored("non-master replica shouldn't receive messages of type ReplicaUpdate");
            return;
        }
        latestTimestamp = latestTimestamp.nextUpdate();
        acksCount.put(latestTimestamp, 0);
        tellBroadcast(MasterUpdate.fromReplicaUpdate(msg, latestTimestamp));
    }

    private void onMasterUpdateMsg(MasterUpdate msg) {
        updatesWaitingForOk.put(msg.timestamp, msg.value);
        tellMaster(ReplicaUpdateAck.fromMasterUpdate(msg));
    }

    private void onReplicaUpdateAckMsg(ReplicaUpdateAck msg) {
        if (!amIMaster()) {
            logMessageIgnored("non-master replica shouldn't receive messages of type ReplicaUpdateAck");
            return;
        }
        Timestamp timestamp = msg.timestamp;
        int updatedCount = acksCount.merge(timestamp, 1, Integer::sum);
        if (updatedCount > getQuorum()) {
            log("quorum for message " + timestamp + " reached");
            acksCount.remove(timestamp);
            tellBroadcast(new MasterUpdateOk(timestamp));
        }
    }

    private void onMasterUpdateOkMsg(MasterUpdateOk msg) {
        if (!updatesWaitingForOk.containsKey(msg.timestamp)) {
            logMessageIgnored("unknown update with timestamp " + msg.timestamp);
            return;
        }
        value = updatesWaitingForOk.remove(msg.timestamp);
        updateHistory.add(msg.timestamp);
        System.out.format("Replica %2d update %d:%d %d\n", id, msg.timestamp.epoch, msg.timestamp.counter, value);
    }

    private void onMasterHeartBeatMsg(MasterHeartBeat msg) {

    }

    private void onReplicaElectionMsg(ReplicaElection msg) {

    }

    private void onMasterSyncMsg(MasterSync msg) {

    }

    private void onReplicaElectionAckMsg(ReplicaElectionAck msg) {

    }

    private void onClientReadMsg(ClientRead msg) {
        getSender().tell(new ReplicaReadReply(value), getSender());
    }

    private boolean amIMaster() {
        return id == masterId;
    }

    private int getQuorum() {
        return Math.floorDiv(replicas.size(), 2) + 1;
    }


    private void tellMaster(Object message) {
        replicas.get(masterId).tell(message, getSelf());
    }

    private void tellBroadcast(Object message) {
        for (ActorRef replica : replicas) {
            replica.tell(message, getSelf());
        }
    }

    private void logMessageIgnored(String reason) {
        log("ignored message: " + reason);
    }

}
