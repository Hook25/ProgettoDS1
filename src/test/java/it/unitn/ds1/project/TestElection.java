package it.unitn.ds1.project;

import it.unitn.ds1.project.Messages.MasterSync;
import it.unitn.ds1.project.Messages.ReplicaElection;
import it.unitn.ds1.project.Messages.ReplicaElectionAck;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.time.Duration;
import java.util.function.Function;

public class TestElection extends MyAkkaTest {

    @Test
    public void testNominal() {
        new MyTestKit(3) {
            {
                sniffer.sendStartMsgFirstScattered();
                within(Duration.ofSeconds(5), () -> {
                    // 0 -> 1
                    sniffer.expectMsgFrom(replicas[1], ReplicaElection.class, replicas[0]);
                    sniffer.expectMsgFrom(replicas[0], ReplicaElectionAck.class, replicas[1]);

                    // 1 -> 2
                    sniffer.expectMsgFrom(replicas[2], ReplicaElection.class, replicas[1]);
                    sniffer.expectMsgFrom(replicas[1], ReplicaElectionAck.class, replicas[2]);

                    // 2 -> 3
                    sniffer.expectMsgFrom(replicas[0], ReplicaElection.class, replicas[2]);
                    sniffer.expectMsgFrom(replicas[2], ReplicaElectionAck.class, replicas[0]);

                    // Sync
                    sniffer.expectMsgFrom(replicas[1], MasterSync.class, replicas[0]);
                    sniffer.expectMsgFrom(replicas[2], MasterSync.class, replicas[0]);

                    return null;
                });
            }
        };
    }
    @Test
    /**
     * Once 0 gets elected it crashes
     * All will now notice the absence of an heartbeat and start another election
     * 1 will now be elected and will send out a MasterSync
     */
    public void testHeartbeatTimeout(){

        new MyTestKit(5){
            {
                Function<Object, Boolean> crashCriteria0 = Messages.MasterSync.class::isInstance;
                Timestamp crashTime0 = new Timestamp(0,1);
                replicas[0].tell(new Messages.CrashPlan(crashTime0, crashCriteria0), null);
                sniffer.sendStartMsg();
                within(Duration.ofSeconds(5), ()->{
                    sniffer.expectMsgFrom(replicas[1], ReplicaElection.class, replicas[0]);
                    sniffer.expectMsgFrom(replicas[1], MasterSync.class, replicas[0]);
                    sniffer.expectMsgFrom(replicas[2], MasterSync.class, replicas[0]);
                    sniffer.expectMsg(replicas[1], ReplicaElection.class);
                    sniffer.expectMsg(replicas[1], MasterSync.class);
                    return  null;
                });
            }
        };
    }
    @Test
    /**
     * Once 0 is elected it crashes
     * All will now start an election but when receiving an election message 2 crashes
     * 2 1 will now be elected and will send out a MasterSync
     */
    public void testElectionAckTimeout(){
        new MyTestKit(5) {
            {
                Function<Object, Boolean> crashCriteria0 = Messages.MasterSync.class::isInstance;
                Timestamp crashTime0 = new Timestamp(0, 1);
                Function<Object, Boolean> crashCriteria2 = Messages.ReplicaElection.class::isInstance;
                Timestamp crashTime2 = new Timestamp(0, 2);

                replicas[0].tell(new Messages.CrashPlan(crashTime0, crashCriteria0), null);
                replicas[2].tell(new Messages.CrashPlan(crashTime2, crashCriteria2), null);

                sniffer.sendStartMsg();

                within(Duration.ofSeconds(15), () -> {
                    sniffer.expectMsg(replicas[0], MasterSync.class);
                    sniffer.expectMsg(replicas[1], ReplicaElection.class);
                    //sniffer.expectMsg(replicas[1], Messages.ReplicaNextDead.class); this is broken?
                    sniffer.expectMsg(replicas[1], ReplicaElection.class);
                    sniffer.expectMsg(replicas[1], MasterSync.class);

                    return null;
                });
            }
        };
    }

}
