package it.unitn.ds1.project;

import akka.actor.ActorRef;
import it.unitn.ds1.project.Messages.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

public class TestTwoPhaseCommit extends MyAkkaTest {


    @Test
    public void clientAsksForValue() {
        new MyTestKit(2) {
            {
                r1.tell(new Start(Arrays.asList(replicas), 5), client);

                within(Duration.ofSeconds(1), () -> {
                    expectClientToRead(r1, 5);
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
                within(Duration.ofSeconds(1), () -> {
                    sniffer.expectMsgFrom(r1, MasterSync.class, master);  // wait for election to finish

                    r1.tell(new ClientUpdate(5), client);
                    sniffer.expectMsgFrom(master, ReplicaUpdate.class, r1);
                    return null;
                });
            }
        };
    }

    @Test
    public void masterBroadcasts_MasterUpdate_uponReceiving_ReplicaUpdate() {
        final int N = 10;
        new MyTestKit(2) {
            {
                sniffer.sendStartMsgFirstScattered();
                within(Duration.ofSeconds(1), () -> {
                    sniffer.expectMsgFrom(r1, MasterSync.class, master); // wait for election to finish

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
                within(Duration.ofSeconds(1), () -> {
                    sniffer.expectMsgFrom(r1, MasterSync.class, master); // wait for election to finish

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
                    sniffer.expectMsgFrom(r1, MasterSync.class, master); // wait for election to finish

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
                    sniffer.expectMsgFrom(r1, MasterSync.class, master); // wait for election to finish

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

}
