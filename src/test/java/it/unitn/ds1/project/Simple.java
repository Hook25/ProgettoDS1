package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.actors.ReplicaActor;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class Simple extends MyAkkaTest {

    @Test
    public void clientAsksForValue() {
        new TestKit(system) {
            {
                List<ActorRef> replicas = Arrays.asList(
                        system.actorOf(ReplicaActor.props(0)),
                        system.actorOf(ReplicaActor.props(1))
                );
                ActorRef firstReplica = replicas.get(0);
                ActorRef client = getRef();

                firstReplica.tell(new Start(replicas, 5), client);

                within(Duration.ofSeconds(1), () -> {

                    firstReplica.tell(new ClientRead(), client);
                    expectMsgEquals(new ReplicaReadReply(5));

                    return null;
                });
            }
        };
    }

    @Test
    public void clientAsksForUpdateAndThenAsksForValue_2replicas() {
        clientAsksForUpdateAndThenAsksForValue(2);
    }


    @Test
    public void clientAsksForUpdateAndThenAsksForValue_5replicas() {
        clientAsksForUpdateAndThenAsksForValue(5);
    }

    public void clientAsksForUpdateAndThenAsksForValue(int replicasCount) {
        new TestKit(system) {
            {
                List<ActorRef> replicas = Arrays.asList(
                        system.actorOf(ReplicaActor.props(0)),
                        system.actorOf(ReplicaActor.props(1))
                );
                ActorRef master = replicas.get(0);
                ActorRef r1 = replicas.get(1);

                Sniffer sniffer = new Sniffer(replicas);
                sniffer.sendStartMsg();

                ActorRef client = getRef();

                within(Duration.ofSeconds(5), () -> {
                    sniffer.expectMsg(master, r1, MasterSync.class);

                    r1.tell(new ClientUpdate(5), client);

                    sniffer.expectMsg(r1, master, ReplicaUpdate.class);

                    for (ActorRef r : replicas) {
                        sniffer.expectMsg(master, r, MasterUpdate.class);
                    }

                    for (ActorRef r : replicas) {
                        sniffer.expectMsg(master, ReplicaUpdateAck.class);
                    }

                    for (ActorRef r : replicas) {
                        sniffer.expectMsg(master, r, MasterUpdateOk.class);
                    }

                    r1.tell(new ClientRead(), client);
                    expectMsgEquals(new ReplicaReadReply(5));

                    return null;
                });
            }
        };
    }

}
