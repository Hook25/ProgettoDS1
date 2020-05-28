package it.unitn.ds1.project;

import akka.actor.ActorRef;
import it.unitn.ds1.project.models.Messages.*;
import it.unitn.ds1.project.models.Timestamp;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.fail;

public class TestTwoPhaseCommit extends TestAPI {


    @Test
    public void clientAsksForValue() {
        new MyTestKit(2) {
            {
                r1.tell(new Start(Arrays.asList(replicas)), client);

                within(Duration.ofSeconds(1), () -> {
                    expectClientToRead(r1, 0);
                    return null;
                });
            }
        };
    }

    @Test
    public void replicaForwardsUpdateRequestToMaster() {
        new MyTestKit(2) {
            {
                sniffer.sendStartMsgFirstScattered();
                within(Duration.ofSeconds(2), () -> {
                    sniffer.waitForFirstElectionToComplete();

                    r1.tell(new ClientUpdate(5), client);
                    sniffer.expectMsgFrom(master, ReplicaUpdate.class, r1);
                    return null;
                });
            }
        };
    }

    @Test
    public void masterBroadcasts_MasterUpdate_uponReceiving_ReplicaUpdate() {
        new MyTestKit(2) {
            {
                sniffer.sendStartMsgFirstScattered();
                within(Duration.ofSeconds(2), () -> {
                    sniffer.waitForFirstElectionToComplete();

                    master.tell(new ReplicaUpdate(5), r1);
                    for (ActorRef replica : replicas) {
                        sniffer.expectMsgFrom(replica, MasterUpdate.class, master);
                    }
                    return null;
                });
            }
        };
    }

    @Test
    public void replicaRepliesTo_MasterUpdate() {
        new MyTestKit(2) {
            {
                sniffer.sendStartMsgFirstScattered();
                within(Duration.ofSeconds(2), () -> {
                    sniffer.waitForFirstElectionToComplete();

                    MasterUpdate mockedMasterUpdate = MasterUpdate.fromReplicaUpdate(new ReplicaUpdate(5), new Timestamp(1, 1));

                    r1.tell(mockedMasterUpdate, master);
                    sniffer.expectMsgFrom(master, ReplicaUpdateAck.class, r1);
                    return null;
                });
            }
        };
    }

//    @Test
//    public void masterBroadcasts_MasterUpdateOk_uponReachingQuorum() {
//        final int N = 5;
//        final int QUORUM = Math.floorDiv(N, 2) + 1;
//        new TestKit(system){{
//            ActorRef master = system.actorOf(ReplicaActor.props(0));
//
//            List<ActorRef> mockedReplicas = new ArrayList<>();
//            for(int i = 0; i < N; i++) {
//                mockedReplicas.add(new TestKit(system).getRef());
//            }
//
//            master.tell(new Start(mockedReplicas, 0), ActorRef.noSender());
//            master.tell(new MasterSync(Collections.emptyList(), 0), ActorRef.noSender());
//
//            master.tell(new ReplicaUpdate(5), ActorRef.noSender());
//
//            for (int i = 0; i < QUORUM; i++) {
//                master.tell(ReplicaUpdateAck.fromMasterUpdate(blabla), ActorRef.noSender());
//            }
//            // TODO: finisci
//        }};
//    }

    @Test
    public void clientAsksForUpdateAndThenAsksForValue_2replicas() {
        new MyTestKit(2) {
            {
                sniffer.sendStartMsgFirstScattered();

                within(Duration.ofSeconds(5), () -> {
                    sniffer.waitForFirstElectionToComplete();

                    r1.tell(new ClientUpdate(5), client);

                    sniffer.expectMsgFrom(master, ReplicaUpdate.class, r1);

                    for (ActorRef r : replicas) {
                        sniffer.expectMsgFrom(r, MasterUpdate.class, master);
                    }

                    for (ActorRef r : replicas) {
                        sniffer.expectMsg(master, ReplicaUpdateAck.class);
                    }

                    for (ActorRef r : replicas) {
                        sniffer.expectMsgFrom(r, MasterUpdateOk.class, master);
                    }

                    expectClientToRead(r1, 5);
                    return null;
                });
            }
        };
    }

    @Test
    public void clientAsksForUpdateAndThenAsksForValue_10replicas() {
        new MyTestKit(10) {
            {
                sniffer.sendStartMsgFirstScattered();

                within(Duration.ofSeconds(5), () -> {
                    sniffer.waitForFirstElectionToComplete();

                    r1.tell(new ClientUpdate(5), client);

                    sniffer.expectMsgFrom(master, ReplicaUpdate.class, r1);

                    sniffer.expectMsgInAnyOrder(master, Arrays.asList(
                            ExpectedMessage.ofClass(MasterUpdate.class),

                            // TODO: rewrite this code without repeating the statement 10 times
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),
                            ExpectedMessage.ofClass(ReplicaUpdateAck.class),

                            ExpectedMessage.ofClass(MasterUpdateOk.class)
                    ));

                    for (int i = 1; i < replicas.length; i++) {
                        sniffer.expectMsgInAnyOrder(replicas[i], Arrays.asList(
                                ExpectedMessage.ofClass(MasterUpdate.class),
                                new ExpectedMessage(new MasterUpdateOk(new Timestamp(1, 1)), master)
                        ));
                    }

                    expectClientToRead(r1, 5);
                    return null;
                });
            }


        };
    }

    @Test
    public void testMasterCrashWhenReceivingReplicaUpdate() {
        new MyTestKit(3) {
            {
                CrashPlan crash = new CrashPlan(new Timestamp(0, 1), ReplicaUpdate.class::isInstance);
                master.tell(crash, ActorRef.noSender());
                sniffer.sendStartMsgFirstScattered();
                within(Duration.ofSeconds(5), () -> {
                    sniffer.waitForFirstElectionToComplete();

                    master.tell(new ReplicaUpdate(5), r1);

                    // r1 should have noticed that master has crashed, and should have started an election
                    sniffer.expectMsgFrom(replicas[2], ReplicaElection.class, r1);
                    return null;
                });
            }
        };
    }

    @Test
    public void testMasterCrashWhenReceivingReplicaUpdateAck() {
        new MyTestKit(3) {
            {
                CrashPlan crash = new CrashPlan(new Timestamp(0, 1), ReplicaUpdateAck.class::isInstance);
                master.tell(crash, ActorRef.noSender());
                sniffer.sendStartMsgFirstScattered();
                within(Duration.ofSeconds(5), () -> {
                    sniffer.waitForFirstElectionToComplete();

                    master.tell(new ReplicaUpdate(5), r1);
                    sniffer.expectMsgFrom(r1, MasterUpdate.class, master);

                    // r1 should have noticed that master has crashed, and should have started (or continued) an election
                    sniffer.expectMsgFrom(replicas[2], ReplicaElection.class, r1);
                    return null;
                });
            }
        };
    }

    @Test
    public void testConcurrentUpdates() {
        final int N_UPDATES = 50;
        new MyTestKit(5) {
            {
                sniffer.sendStartMsgFirstScattered();
                sniffer.waitForFirstElectionToComplete();

                Thread updater = new Thread(() -> {
                    for (int i = 0; i < N_UPDATES; i++) {
                        r1.tell(new ClientUpdate(i), client);
                        try {
                            Thread.sleep(ThreadLocalRandom.current().nextInt(250));
                        } catch (InterruptedException e) {

                        }
                    }
                });

                Thread checker = new Thread(() -> {
                    int lastReadValue = -1;
                    do {
                        r1.tell(new ClientRead(), client);
                        ReplicaReadReply reply = expectMsgClass(ReplicaReadReply.class);
                        if (reply.value < lastReadValue) {
                            System.exit(-1);
                        }
                        lastReadValue = reply.value;
                    } while (lastReadValue != N_UPDATES - 1);
                });

                checker.start();
                updater.start();
                try {
                    checker.join();
                } catch (InterruptedException e) {
                    fail();
                }
            }
        };
    }

}
