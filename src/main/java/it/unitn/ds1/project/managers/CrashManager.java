package it.unitn.ds1.project.managers;

import akka.actor.AbstractActor.Receive;
import it.unitn.ds1.project.actors.ReplicaActor;
import it.unitn.ds1.project.models.Timestamp;

import java.util.function.BiFunction;

public class CrashManager {
    private Timestamp crashTime;
    private final ReplicaActor replica;
    private BiFunction<ReplicaActor, Object, Boolean> crashCriteria;
    private Receive receiver;

    public CrashManager(ReplicaActor replica) {
        this.replica = replica;
    }

    public void setReceiver(Receive receiver) {
        this.receiver = receiver;
    }

    public void setTimestamp(Timestamp ts) {
        this.crashTime = ts;
    }

    public void setCrashCriteria(BiFunction<ReplicaActor, Object, Boolean> crashCriteria) {
        this.crashCriteria = crashCriteria;
    }

    public void consume(Object message) {
        if (shouldCrash(message)) {
            replica.getContext().become(crashed());
            replica.log("CRASHED");
        } else {
            receiver.onMessage().apply(message);
        }
    }

    private boolean shouldCrash(Object message) {
        return replica.getLatestUpdate().timestamp.equals(crashTime) &&
                crashCriteria != null && crashCriteria.apply(this.replica, message);
    }

    private Receive crashed() {
        return replica.receiveBuilder()
                .matchAny(msg -> {})
                .build();
    }
}
