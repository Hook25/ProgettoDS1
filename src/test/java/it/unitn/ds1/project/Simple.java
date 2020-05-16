package it.unitn.ds1.project;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.TestActor;
import akka.testkit.javadsl.TestKit;
import it.unitn.ds1.project.actors.ClientActor;
import it.unitn.ds1.project.actors.ReplicaActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import it.unitn.ds1.project.Messages.*;


import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;

public class Simple {
    private ActorSystem system;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create();
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

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
    public void clientAsksForUpdateAndThenAsksForValue() {
        new TestKit(system) {
            {
                ActorRef a = system.actorOf(ReplicaActor.props(0));
                ActorRef b = system.actorOf(ReplicaActor.props(1));

                TestKit aProbe = proxify(a);

                List<ActorRef> replicas = Arrays.asList(
                        aProbe.getRef(),
                        b
                );

                ActorRef client = getRef();
                a.tell(new Start(replicas, 5), client);
                b.tell(new Start(replicas, 5), client);

                within(Duration.ofSeconds(1), () -> {

                    a.tell(new ClientUpdate(10), client);
                    aProbe.ignoreMsg(msg -> !(msg instanceof MasterUpdateOk));
                    aProbe.expectMsgClass(MasterUpdateOk.class);

                    a.tell(new ClientRead(), client);
                    expectMsgEquals(new ReplicaReadReply(10));

                    return null;
                });
            }


            private TestKit proxify(ActorRef toProxy) {
                TestKit probe = new TestKit(system);
                probe.setAutoPilot(
                        new TestActor.AutoPilot() {
                            public TestActor.AutoPilot run(ActorRef sender, Object msg) {
                                toProxy.tell(msg, sender);
                                return this;
                            }
                        });
                return probe;
            }
        };
    }


}
