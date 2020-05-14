package it.unitn.ds1.project.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Messages.ReplicaReadReply;

import java.util.List;

public class ClientActor extends ActorWithId {

    private final List<ActorRef> replicas;

    static public Props props(int id, List<ActorRef> replicas) {
        return Props.create(ClientActor.class, () -> new ClientActor(id, replicas));
    }

    public ClientActor(int id, List<ActorRef> replicas) {
        super("Client", id);
        this.replicas = replicas;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ReplicaReadReply.class, this::onReplicaReadReplyMsg)
                .build();
    }

    private void onReplicaReadReplyMsg(ReplicaReadReply msg) {
        log("read done " + msg.value);
    }
}
