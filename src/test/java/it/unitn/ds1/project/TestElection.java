package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import it.unitn.ds1.project.Messages.MasterSync;
import it.unitn.ds1.project.Messages.ReplicaElection;
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
                    sniffer.expectMsg(replicas.get(0), replicas.get(1), ReplicaElection.class);
                    sniffer.expectMsg(replicas.get(1), replicas.get(2), ReplicaElection.class);
                    sniffer.expectMsg(replicas.get(2), replicas.get(0), ReplicaElection.class);

                    /*
                     * TODO: Can we find a way to test also the acks?
                     * currently we can't, because in ElectionDelegate, the replica replies to the ElectionMessage
                     * calling .getSender(), which will return the real replica instead of the TestKit (since our
                     * ForwarderProbe specifies the real sender when it forwards the message).
                     *
                     */


                    // Sync
                    sniffer.expectMsg(replicas.get(0), replicas.get(1), MasterSync.class);
                    sniffer.expectMsg(replicas.get(0), replicas.get(2), MasterSync.class);

                    return null;
                });
            }
        };
    }

}
