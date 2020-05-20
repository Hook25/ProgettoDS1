package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import it.unitn.ds1.project.actors.ClientActor;
import it.unitn.ds1.project.actors.ReplicaActor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Main {

    private static final int N_REPLICAS = 3;

    public static void main(String[] args) throws IOException {
        final ActorSystem system = ActorSystem.create("quorumTotalOrder");

        List<ActorRef> replicas = new ArrayList<>();
        for (int i = 0; i < N_REPLICAS; i++) {
            ActorRef replicaI = system.actorOf(ReplicaActor.props(i));
            replicas.add(replicaI);
        }

        Messages.Start startMessage = new Messages.Start(replicas, 0);
        for (ActorRef replica : replicas) {
            replica.tell(startMessage, null);
        }


        ActorRef client = system.actorOf(ClientActor.props(50, replicas));

        Function<Object, Boolean> crashCriteria = Messages.ClientRead.class::isInstance;

        System.out.println(">>> Press ENTER to crash 0 <<<");
        System.in.read();
        replicas.get(0).tell(new Messages.CrashPlan(new Timestamp(0, 1), crashCriteria), null);
        replicas.get(0).tell(new Messages.ClientRead(), client);
        //replicas.get(0).tell(new Messages.CrashPlanner(new Timestamp(1,1), tmp), null);
        System.out.println(">>> Press ENTER to write <<<");
        System.in.read();
        replicas.get(0).tell(new Messages.ClientUpdate(5), client);
        replicas.get(0).tell(new Messages.ClientRead(), client);
        System.out.println(">>> Press ENTER to exit <<<");
        System.in.read();
        system.terminate();
    }
}
