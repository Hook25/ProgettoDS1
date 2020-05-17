package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestActor;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.unitn.ds1.project.actors.ReplicaActor.HEARTBEAT_TIMEOUT_MS;
import static org.junit.jupiter.api.Assertions.fail;

public class MyAkkaTest {
    protected ActorSystem system;

    @BeforeEach
    public void setup() {
        system = ActorSystem.create();
    }

    @AfterEach
    public void teardown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    public class Sniffer {
        private final List<ActorRef> replicas;
        private final Map<ActorRef, TestKit> probeByReplica;
        private final List<ActorRef> forwarders;

        public Sniffer(List<ActorRef> replicas) {
            this.replicas = replicas;
            probeByReplica = replicas.stream()
                    .map(r -> new ForwarderProbe(system, r))
                    .collect(Collectors.toMap(probe -> probe.receiver, probe -> probe));
            forwarders = replicas
                    .stream()
                    .map(probeByReplica::get)
                    .map(TestKit::getRef)
                    .collect(Collectors.toList());
        }


        public void sendStartMsg() {
            for (ActorRef r : replicas) {
                r.tell(new Messages.Start(forwarders, 0), ActorRef.noSender());
            }
        }


        public void sendStartMsgFirstScattered() {
            for (int i = 0; i < replicas.size(); i++) {
                ActorRef r = replicas.get(i);
                r.tell(new Messages.Start(forwarders, 0), ActorRef.noSender());

                if (i == 0) {
                    try {
                        Thread.sleep(HEARTBEAT_TIMEOUT_MS - 100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void expectMsg(ActorRef to, Class<?> msgClass) {
            TestKit probe = probeByReplica.get(to);
            waitForMessageFrom(probe, msgClass, null);
        }

        public void expectMsg(ActorRef from, ActorRef to, Class<?> msgClass) {
            TestKit probe = probeByReplica.get(to);
            waitForMessageFrom(probe, msgClass, from);
        }

        private Object waitForMessageFrom(TestKit probe, Class<?> msgClass, ActorRef sender) {
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 3000) {
                Object msg = probe.receiveN(1).get(0);
                if (msgClass.isInstance(msg) && (sender == null || probe.getLastSender().equals(sender))) {
                    return msg;
                }
                System.out.println(msg);
            }
            fail();
            return null;
        }

    }

    public static class ForwarderProbe extends TestKit {
        final ActorRef receiver;

        public ForwarderProbe(ActorSystem system, ActorRef receiver) {
            super(system);
            this.receiver = receiver;
            setAutoPilot(new TestActor.AutoPilot() {
                public TestActor.AutoPilot run(ActorRef sender, Object msg) {
                    receiver.tell(msg, sender);
                    return this;
                }
            });
        }
    }

}
