package it.unitn.ds1.project.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Messages.*;

public class ReplicaActor extends AbstractActor {

    static public Props props(ActorRef manager, boolean joining) {
        return Props.create(ReplicaActor.class, ReplicaActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
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
                .match(ReplicaReadReply.class, this::onReplicaReadReplyMsg)
                .build();
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

    }

    private void onReplicaReadReplyMsg(ReplicaReadReply msg) {

    }


}
