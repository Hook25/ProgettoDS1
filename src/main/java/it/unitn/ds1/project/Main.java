package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import it.unitn.ds1.project.actors.ReplicaActor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static int N_REPLICAS = 10;

    public static void main(String[] args) throws IOException {
        final ActorSystem system = ActorSystem.create("quorumTotalOrder");

        List<ActorRef> replicas = new ArrayList<>();

        ActorRef initialMaster = system.actorOf(ReplicaActor.props(0, true));
        replicas.add(initialMaster);

        for (int i = 1; i < N_REPLICAS; i++) {
            ActorRef replicaI = system.actorOf(ReplicaActor.props(i, false));
            replicas.add(replicaI);
        }


        Messages.Start startMessage = new Messages.Start(replicas, 0, 0);
        for (ActorRef replica : replicas) {
            replica.tell(startMessage, null);
        }

        System.out.println(">>> Press ENTER to exit <<<");
        System.in.read();
        system.terminate();
    }
}
