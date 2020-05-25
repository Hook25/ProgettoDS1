package it.unitn.ds1.project;

import akka.actor.ActorRef;
import it.unitn.ds1.project.Messages.MasterSync;
import it.unitn.ds1.project.Messages.ReplicaElection;
import it.unitn.ds1.project.Messages.ReplicaElectionAck;
import it.unitn.ds1.project.actors.ReplicaActor;
import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.time.Duration;
import java.util.function.BiFunction;
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

    /**
     * Once 0 gets elected it crashes
     * All will now notice the absence of an heartbeat and start another election
     * 1 will now be elected and will send out a MasterSync
     */
    @Test
    public void testHeartbeatTimeout(){

        new MyTestKit(5){
            {
                BiFunction<ReplicaActor, Object, Boolean> crashCriteria0 = (me, msg) -> Messages.MasterSync.class.isInstance(msg);
                Timestamp crashTime0 = new Timestamp(0,1);
                replicas[0].tell(new Messages.CrashPlan(crashTime0, crashCriteria0), null);
                sniffer.sendStartMsg();
                within(Duration.ofSeconds(50), ()->{
                    /*sniffer.expectMsgFrom(replicas[1], ReplicaElection.class, replicas[0]);
                    sniffer.expectMsgFrom(replicas[1], MasterSync.class, replicas[0]);
                    sniffer.expectMsgFrom(replicas[2], MasterSync.class, replicas[0]);*/
                    sniffer.expectMsg(replicas[1], MasterSync.class);
                    sniffer.expectMsg(replicas[1], ReplicaElection.class);
                    sniffer.expectMsg(replicas[1], MasterSync.class);
                    return  null;
                });
            }
        };
    }

    /**
     * Once 0 is elected it crashes
     * All will now start an election but when receiving an election message 2 crashes
     * 1 will now be elected and will send out a MasterSync
     */
    @Test
    public void testElectionAckTimeout(){
        new MyTestKit(5) {
            {
                BiFunction<ReplicaActor, Object, Boolean> crashCriteria0 = (me, msg) -> Messages.MasterSync.class.isInstance(msg);
                Timestamp crashTime0 = new Timestamp(0, 1);
                BiFunction<ReplicaActor, Object, Boolean>  crashCriteria2 = (me, msg) -> Messages.ReplicaElection.class.isInstance(msg);
                Timestamp crashTime2 = new Timestamp(0, 2);

                replicas[0].tell(new Messages.CrashPlan(crashTime0, crashCriteria0), null);
                replicas[2].tell(new Messages.CrashPlan(crashTime2, crashCriteria2), null);

                sniffer.sendStartMsg();

                within(Duration.ofSeconds(50), () -> {
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

    /**
     * Once 0 is elected it crashes
     * All will now start an election but when receiving an election message sends
     * an ack and then crashes
     * 1 will now be elected and will send out a MasterSync
     */
    @Test
    public void testElectionAckAndFail(){
        new MyTestKit(5) {
            {
                BiFunction<ReplicaActor, Object, Boolean> crashCriteria0 = (me, msg) -> Messages.MasterSync.class.isInstance(msg);
                Timestamp crashTime0 = new Timestamp(0, 1);
                BiFunction<ReplicaActor, Object, Boolean>  crashCriteria2 = (me, msg) -> {
                    if(Messages.ReplicaElection.class.isInstance(msg)){
                        ActorRef ar = me.getSender();
                        assert ar != null : "actor ref in getsender returned null";
                        ReplicaElection act_msg = (ReplicaElection)msg;
                        ar.tell(new ReplicaElectionAck(act_msg.id), me.getSelf());
                        return true;
                    }
                    return false;
                };
                Timestamp crashTime2 = new Timestamp(0, 2);

                replicas[0].tell(new Messages.CrashPlan(crashTime0, crashCriteria0), null);
                replicas[2].tell(new Messages.CrashPlan(crashTime2, crashCriteria2), null);

                sniffer.sendStartMsg();

                within(Duration.ofSeconds(50), () -> {
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

    /**
     * Once 0 is elected it crashes
     * All will now start an election but when receiving an election message sends
     * an ack and then crashes
     * 1 will now be elected and will send out a MasterSync
     */
    @Test
    public void testElectionAckAndFailShouldMaster(){
        new MyTestKit(5) {
            {
                BiFunction<ReplicaActor, Object, Boolean> crashCriteria0 = (me, msg) -> Messages.MasterSync.class.isInstance(msg);
                Timestamp crashTime0 = new Timestamp(0, 1);
                BiFunction<ReplicaActor, Object, Boolean>  crashCriteria2 = (me, msg) -> {
                    if(Messages.ReplicaElection.class.isInstance(msg)){
                        ActorRef ar = me.getSender();
                        assert ar != null : "actor ref in getsender returned null";
                        ReplicaElection act_msg = (ReplicaElection)msg;
                        ar.tell(new ReplicaElectionAck(act_msg.id), me.getSelf());
                        return true;
                    }
                    return false;
                };
                Timestamp crashTime2 = new Timestamp(0, 2);

                replicas[0].tell(new Messages.CrashPlan(crashTime0, crashCriteria0), null);
                replicas[1].tell(new Messages.CrashPlan(crashTime2, crashCriteria2), null);

                sniffer.sendStartMsg();

                within(Duration.ofSeconds(50), () -> {
                    sniffer.expectMsg(replicas[0], MasterSync.class);
                    sniffer.expectMsg(replicas[1], ReplicaElection.class);
                    //sniffer.expectMsg(replicas[1], Messages.ReplicaNextDead.class); this is broken?
                    sniffer.expectMsg(replicas[1], ReplicaElection.class);
                    sniffer.expectMsg(replicas[2], MasterSync.class);
                    return null;
                });
            }
        };
    }

}
