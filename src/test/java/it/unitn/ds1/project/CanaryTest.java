package it.unitn.ds1.project;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple test to verify that JUnit and Akka testkit are setup correctly.
 * Akka test is copied from https://doc.akka.io/docs/akka/current/testing.html
 */
public class CanaryTest {

    @Test
    public void testJUnit() {
        assertTrue(true);
    }

    public static class SomeActor extends AbstractActor {
        ActorRef target = null;

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchEquals(
                            "hello",
                            message -> {
                                getSender().tell("world", getSelf());
                                if (target != null) target.forward(message, getContext());
                            })
                    .match(
                            ActorRef.class,
                            actorRef -> {
                                target = actorRef;
                                getSender().tell("done", getSelf());
                            })
                    .build();
        }
    }

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
    public void testIt() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new TestKit(system) {
            {
                final ActorRef subject = system.actorOf(Props.create(SomeActor.class));

                // can also use JavaTestKit “from the outside”
                final TestKit probe = new TestKit(system);
                // “inject” the probe by passing it to the test subject
                // like a real resource would be passed in production
                subject.tell(probe.getRef(), getRef());
                // await the correct response
                expectMsg(Duration.ofSeconds(1), "done");

                // the run() method needs to finish within 3 seconds
                within(
                        Duration.ofSeconds(3),
                        () -> {
                            subject.tell("hello", getRef());

                            // This is a demo: would normally use expectMsgEquals().
                            // Wait time is bounded by 3-second deadline above.
                            awaitCond(probe::msgAvailable);

                            // response must have been enqueued to us before probe
                            expectMsg(Duration.ZERO, "world");
                            // check that the probe we injected earlier got the msg
                            probe.expectMsg(Duration.ZERO, "hello");
                            assertEquals(getRef(), probe.getLastSender());

                            // Will wait for the rest of the 3 seconds
                            expectNoMessage();
                            return null;
                        });
            }
        };
    }
}
