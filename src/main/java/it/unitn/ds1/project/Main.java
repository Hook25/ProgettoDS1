package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import it.unitn.ds1.project.actors.ClientActor;
import it.unitn.ds1.project.actors.ReplicaActor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

public class Main {

    private static final int N_REPLICAS = 10;

    public static void main(String[] args) throws IOException, InterruptedException {
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


        replicas.get(0).tell(new Messages.CrashPlan(
                new Timestamp(1, 1),
                Messages.ReplicaUpdate.class::isInstance
        ), null);

        ActorRef client = system.actorOf(ClientActor.props(50, replicas));


        String command;
        Scanner scanner = new Scanner(System.in);
        do {
            Thread.sleep(1000);
            System.out.println("\nType code and press ENTER");
            System.out.println("e - Exit");
            System.out.println("u - Update: u <value> <replica>");
            System.out.println("r - Read: r <replica>");

            command = scanner.next(".");
            int replicaId;
            switch (command) {
                case "u":
                    int value = scanner.nextInt();
                    replicaId = scanner.nextInt();
                    replicas.get(replicaId).tell(new Messages.ClientUpdate(value), client);
                    break;
                case "r":
                    replicaId = scanner.nextInt();
                    replicas.get(replicaId).tell(new Messages.ClientRead(), client);
                    break;
            }
        } while (!"e".equals(command));

        system.terminate();
    }
}
