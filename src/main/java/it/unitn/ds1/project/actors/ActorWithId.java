package it.unitn.ds1.project.actors;

import akka.actor.AbstractActor;

public abstract class ActorWithId extends AbstractActor {

    private final String actorType;

    protected final int id;

    public ActorWithId(String actorType, int id) {
        this.actorType = actorType;
        this.id = id;
    }

    public void log(String message) {
        System.out.format("%s %2d %s\n", actorType, id, message);
    }

}
