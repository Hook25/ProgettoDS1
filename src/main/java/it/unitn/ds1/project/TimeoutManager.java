package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import it.unitn.ds1.project.Messages.Ack;
import it.unitn.ds1.project.Messages.AcknowledgeableMessage;
import it.unitn.ds1.project.Messages.MessageId;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TimeoutManager {

    private final ActorRef replica;
    private final ActorSystem system;

    private final Map<MessageId, Cancellable> timeouts = new HashMap<>();

    public TimeoutManager(ActorRef replica, ActorSystem system) {
        this.replica = replica;
        this.system = system;
    }

    public void startTimeout(AcknowledgeableMessage<? extends MessageId> msg, int time, Object toDeliver) {
        Cancellable cancellable = setTimeout(time, toDeliver);
        timeouts.put(msg.id, cancellable);
    }

    public void cancelTimeout(Ack<? extends MessageId> msg) {
        MessageId id = msg.acknowledgedId;
        if (timeouts.containsKey(id)) {
            timeouts.remove(id).cancel();
        }
    }

    private Cancellable setTimeout(int time, Object toDeliver) {
        return system.scheduler().scheduleOnce(
                Duration.create(time, TimeUnit.MILLISECONDS),
                replica,
                toDeliver,
                system.dispatcher(),
                replica
        );
    }

}
