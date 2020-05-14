package it.unitn.ds1.project.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Messages.*;

import java.util.List;

public class ReplicaActor extends ActorWithId {

    private int value;

    private List<ActorRef> replicas;

    private int masterId;

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

    }

    private void onReplicaUpdateMsg(ReplicaUpdate msg) {

    }

    private void onMasterUpdateMsg(MasterUpdate msg) {

    }

    private void onReplicaUpdateAckMsg(ReplicaUpdateAck msg) {

    }

    private void onMasterUpdateOkMsg(MasterUpdateOk msg) {

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
