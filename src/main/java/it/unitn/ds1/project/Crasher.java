package it.unitn.ds1.project;

import akka.actor.AbstractActor.Receive;
import it.unitn.ds1.project.actors.ReplicaActor;

import java.util.function.Function;

public class Crasher {
    private Timestamp crashTime;
    private final ReplicaActor replica;
    private Function<Object, Boolean> crashCriteria;
    private Receive receiver;

    public Crasher(ReplicaActor replica) {
        this.replica = replica;
    }

    public void setReceiver(Receive receiver) {
        this.receiver = receiver;
    }

    public void setTimestamp(Timestamp ts) {
        this.crashTime = ts;
    }

    public void setCrashCriteria(Function<Object, Boolean> crashCriteria) {
        this.crashCriteria = crashCriteria;
    }

    public void consume(Object message) {
        if (shouldCrash(message)) {
            replica.getContext().become(crashed());
            replica.log("crashed");
        } else {
            receiver.onMessage().apply(message);
        }
    }

    private boolean shouldCrash(Object message) {
        return replica.getLatestUpdate().equals(crashTime) &&
                crashCriteria != null && crashCriteria.apply(message);
    }

    private Receive crashed() {
        return replica.receiveBuilder()
                .matchAny(msg -> {})
                .build();
    }
}
