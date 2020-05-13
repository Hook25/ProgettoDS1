package it.unitn.ds1.project.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ClientActor extends AbstractActor {

    static public Props props(ActorRef manager, boolean joining) {
        return Props.create(ClientActor.class, ClientActor::new);
    }

    @Override
    public Receive createReceive() {
        return null;
    }

}
