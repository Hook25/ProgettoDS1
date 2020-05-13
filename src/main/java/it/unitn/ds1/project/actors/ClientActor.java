package it.unitn.ds1.project.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Messages.*;

import java.util.List;

public class ClientActor extends AbstractActor {

    private final List<ActorRef> replicas;

    static public Props props(List<ActorRef> replicas) {
        return Props.create(ClientActor.class, () -> new ClientActor(replicas));
    }

    public ClientActor(List<ActorRef> replicas) {
        this.replicas = replicas;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ReplicaReadReply.class, this::onReplicaReadReplyMsg)
                .build();
    }

    private void onReplicaReadReplyMsg(ReplicaReadReply msg) {

    }
}
