package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import it.unitn.ds1.project.actors.ClientActor;
import it.unitn.ds1.project.actors.ReplicaActor;
import it.unitn.ds1.project.models.Messages;
import it.unitn.ds1.project.models.Timestamp;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;


public class Main {

    private static final int N_REPLICAS = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
        final ActorSystem system = ActorSystem.create("quorumTotalOrder");
        int nodeId = 0;

        List<ActorRef> replicas = new ArrayList<>();
        for (int i = 0; i < N_REPLICAS; i++) {
            ActorRef replicaI = system.actorOf(ReplicaActor.props(nodeId++));
            replicas.add(replicaI);
        }

        Messages.Start startMessage = new Messages.Start(replicas);
        for (ActorRef replica : replicas) {
            replica.tell(startMessage, null);
        }


        nodeId = 50;
        startReadingClient(nodeId++, replicas, system);
        startUpdatingClient(nodeId++, replicas, system);

        ActorRef client = system.actorOf(ClientActor.props(nodeId++, replicas));

        BiFunction<ReplicaActor, Object, Boolean> crashCriteria = (me, msg) -> true;
        replicas.get(0).tell(new Messages.CrashPlan(new Timestamp(3, 1), crashCriteria), null);
        replicas.get(0).tell(new Messages.ClientRead(), client);
        System.out.println(">>> Press ENTER to write <<<");
        System.in.read();
        replicas.get(0).tell(new Messages.ClientUpdate(5), client);
        replicas.get(0).tell(new Messages.ClientRead(), client);
        System.out.println(">>> Press ENTER to exit <<<");
        System.in.read();
        system.terminate();
    }

    /**
     * This method starts a client that will ask repeatedly ask to read a value form the same replica.
     * The purpose of this method is to show  the correct ordering of updates.
     */
    private static void startReadingClient(int id, List<ActorRef> replicas, ActorSystem system) {
        ActorRef client = system.actorOf(ClientActor.props(id, replicas));
        FiniteDuration duration = Duration.create(1000, TimeUnit.MILLISECONDS);
        system.scheduler().scheduleAtFixedRate(
                duration,
                duration,
                () -> client.tell(new Messages.ReminderClientRead(1), ActorRef.noSender()),
                system.dispatcher()
        );
    }

    /**
     * This method start a client that will ask repeatedly to update a value to the same replice.
     * The purpose of this method is to show  the correct ordering of updates.
     */
    private static void startUpdatingClient(int id, List<ActorRef> replicas, ActorSystem system) {
        ActorRef client = system.actorOf(ClientActor.props(id, replicas));
        FiniteDuration duration = Duration.create(5000, TimeUnit.MILLISECONDS);
        // here we use an atomic integer because we need an effective final variable,
        // otherwise we cannot use it inside the lambda.
        AtomicInteger value = new AtomicInteger(1);
        system.scheduler().scheduleAtFixedRate(
                duration,
                duration,
                () -> client.tell(new Messages.ReminderClientUpdate(1, value.incrementAndGet()), ActorRef.noSender()),
                system.dispatcher()
        );
    }
}
