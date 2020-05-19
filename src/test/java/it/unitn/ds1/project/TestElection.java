package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.actors.ReplicaActor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TestElection extends MyAkkaTest {

    @Test
    public void testInitialElection() {
        new TestKit(system) {
            {
                List<ActorRef> replicas = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    replicas.add(system.actorOf(ReplicaActor.props(i)));
                }

                Sniffer sniffer = new Sniffer(replicas);
                sniffer.sendStartMsgFirstScattered();

                within(Duration.ofSeconds(5), () -> {
                    // 0 -> 1
                    sniffer.expectMsg(replicas.get(0), replicas.get(1), ReplicaElection.class);
                    sniffer.expectMsg(replicas.get(1), replicas.get(0), ReplicaElectionAck.class);

                    // 1 -> 2
                    sniffer.expectMsg(replicas.get(1), replicas.get(2), ReplicaElection.class);
                    sniffer.expectMsg(replicas.get(2), replicas.get(1), ReplicaElectionAck.class);

                    // 2 -> 3
                    sniffer.expectMsg(replicas.get(2), replicas.get(0), ReplicaElection.class);
                    sniffer.expectMsg(replicas.get(0), replicas.get(2), ReplicaElectionAck.class);

                    // Sync
                    sniffer.expectMsg(replicas.get(0), replicas.get(1), MasterSync.class);
                    sniffer.expectMsg(replicas.get(0), replicas.get(2), MasterSync.class);

                    return null;
                });
            }
        };
    }

}
