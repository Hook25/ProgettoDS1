package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import it.unitn.ds1.project.actors.ClientActor;
import it.unitn.ds1.project.actors.ReplicaActor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;


public class Main {

    private static final int N_REPLICAS = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
        final ActorSystem system = ActorSystem.create("quorumTotalOrder");

        List<ActorRef> replicas = new ArrayList<>();
        for (int i = 0; i < N_REPLICAS; i++) {
            ActorRef replicaI = system.actorOf(ReplicaActor.props(i));
            replicas.add(replicaI);
        }

        Messages.Start startMessage = new Messages.Start(replicas);
        for (ActorRef replica : replicas) {
            replica.tell(startMessage, null);
        }


        replicas.get(0).tell(new Messages.CrashPlan(
                new Timestamp(1, 1),
                Messages.ReplicaUpdate.class::isInstance
        ), null);

        ActorRef client = system.actorOf(ClientActor.props(50, replicas));
        BiFunction<ReplicaActor, Object, Boolean> crashCriteria = (me, msg) -> Messages.ClientRead.class.isInstance(msg);

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
