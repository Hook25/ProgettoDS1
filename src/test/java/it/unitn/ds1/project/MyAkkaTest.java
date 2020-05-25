package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestActor;
import akka.testkit.javadsl.TestKit;
import it.unitn.ds1.project.actors.ReplicaActor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static it.unitn.ds1.project.actors.HeartbeatDelegate.HEARTBEAT_TIMEOUT_MS;
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

    protected ActorRef[] createReplicas(int n) {
        ActorRef[] replicas = new ActorRef[n];
        for (int i = 0; i < n; i++) {
            replicas[i] = system.actorOf(ReplicaActor.props(i));
        }
        return replicas;
    }

    protected abstract class MyTestKit extends TestKit {
        protected final ActorRef[] replicas;
        protected final ActorRef master;
        protected final ActorRef r1;
        protected final Sniffer sniffer;
        protected final ActorRef client = getRef();

        public MyTestKit(int n) {
            super(system);
            if (n < 2) {
                throw new IllegalArgumentException("number of replicas must be >= 2");
            }
            replicas = createReplicas(n);
            master = replicas[0];
            r1 = replicas[1];

            sniffer = new Sniffer(replicas);

        }

        protected void expectClientToRead(ActorRef replica, int expectedValue) {
            replica.tell(new Messages.ClientRead(), getRef());
            expectMsgEquals(new Messages.ReplicaReadReply(expectedValue));
        }

    }

    public class Sniffer {
        private final ActorRef[] replicas;
        private final Map<ActorRef, TestKit> probeByReplica;
        private final List<ActorRef> forwarders;

        public Sniffer(ActorRef[] replicas) {
            this.replicas = replicas;
            probeByReplica = Arrays.stream(replicas)
                    .map(r -> new ForwarderProbe(system, r))
                    .collect(Collectors.toMap(probe -> probe.receiver, probe -> probe));
            forwarders = Arrays.stream(replicas)
                    .map(probeByReplica::get)
                    .map(TestKit::getRef)
                    .collect(Collectors.toList());
        }


        public void sendStartMsg() {
            for (ActorRef r : replicas) {
                r.tell(new Messages.Start(forwarders), ActorRef.noSender());
            }
        }


        public void sendStartMsgFirstScattered() {
            for (int i = 0; i < replicas.length; i++) {
                ActorRef r = replicas[i];
                r.tell(new Messages.Start(forwarders), ActorRef.noSender());

                if (i == 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        protected void waitForFirstElectionToComplete(){
            for (ActorRef replica : replicas) {
                expectMsg(replica, Messages.MasterSync.class);
            }
        }

        public void expectMsg(ActorRef receiver, Class<?> msgClass) {
            expectMsg(receiver, ExpectedMessage.ofClass(msgClass));
        }

        public void expectMsgFrom(ActorRef receiver, Class<?> msgClass, ActorRef from) {
            expectMsg(receiver, ExpectedMessage.ofClass(msgClass, from));
        }

        public void expectMsg(ActorRef receiver, ExpectedMessage expectedMessage) {
            expectMsgInAnyOrder(receiver, Collections.singletonList(expectedMessage));
        }

        public void expectMsgInAnyOrder(ActorRef receiver, List<ExpectedMessage> expectedMessages) {
            List<ExpectedMessage> remainingExpectedMessages = new ArrayList<>(expectedMessages);
            TestKit probe = probeByReplica.get(receiver);
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 30000 && !remainingExpectedMessages.isEmpty()) {
                Object msg = probe.receiveN(1, Duration.ofSeconds(30)).get(0);

                for (Iterator<ExpectedMessage> i = remainingExpectedMessages.iterator(); i.hasNext(); ) {
                    if (i.next().isExpectedMessage(msg, probe.getLastSender())) {
                        i.remove();
                        break;
                    }
                }
            }
            if (!remainingExpectedMessages.isEmpty()) {
                System.out.println("needed to wait for " + expectedMessages + ", still waiting for " + remainingExpectedMessages);
                fail();
            }
        }

        public class ForwarderProbe extends TestKit {
            final ActorRef receiver;

            public ForwarderProbe(ActorSystem system, ActorRef receiver) {
                super(system);
                this.receiver = receiver;
                setAutoPilot(new TestActor.AutoPilot() {
                    public TestActor.AutoPilot run(ActorRef sender, Object msg) {
                        ActorRef simulatedSender = probeByReplica.get(sender).getRef();
                        receiver.tell(msg, simulatedSender);
                        return this;
                    }
                });
            }
        }
    }


    public static class ExpectedMessage {
        private final Class<?> expectedClass;
        private final Object expectedInstance;
        private final ActorRef expectedSender;

        public static ExpectedMessage ofClass(Class<?> msgClass) {
            return new ExpectedMessage(msgClass, null, null);
        }

        public static ExpectedMessage ofClass(Class<?> msgClass, ActorRef from) {
            return new ExpectedMessage(msgClass, null, from);
        }

        private ExpectedMessage(Class<?> msgClass, Object expectedInstance, ActorRef sender) {
            this.expectedClass = msgClass;
            this.expectedInstance = expectedInstance;
            this.expectedSender = sender;
        }

        public ExpectedMessage(Object expectedInstance, ActorRef sender) {
            this.expectedClass = expectedInstance.getClass();
            this.expectedInstance = expectedInstance;
            this.expectedSender = sender;
        }

        boolean isExpectedMessage(Object msg, ActorRef sender) {
            return expectedClass.isInstance(msg) &&
                    (expectedInstance == null || expectedInstance.equals(msg)) &&
                    (expectedSender == null || expectedSender.equals(sender));
        }

        @Override
        public String toString() {
            return "ExpectedMessage{" +
                    "expectedClass=" + expectedClass +
                    ", expectedInstance=" + expectedInstance +
                    ", expectedSender=" + expectedSender +
                    '}';
        }
    }
}
