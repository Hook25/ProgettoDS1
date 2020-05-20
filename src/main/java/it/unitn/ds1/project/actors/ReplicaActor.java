package it.unitn.ds1.project.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.TimeoutManager;
import it.unitn.ds1.project.Timestamp;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ReplicaActor extends ActorWithId {

    private int value;

    private List<ActorRef> replicas;

    private int masterId;

    private final int id;

    private final TimeoutManager timeoutManager = new TimeoutManager(getSelf(), getContext().system());

    private final TwoPhaseCommitDelegate twoPhaseCommitDelegate = new TwoPhaseCommitDelegate(this);
    private final ElectionDelegate electionDelegate = new ElectionDelegate(this);
    private final HeartbeatDelegate heartbeatDelegate = new HeartbeatDelegate(this);

    private Timestamp latestUpdate = new Timestamp(0, 0);

    static public Props props(int id) {
        return Props.create(ReplicaActor.class, () -> new ReplicaActor(id));
    }

    public ReplicaActor(int id) {
        super("Replica", id);
        this.id = id;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Start.class, this::onStartMsg)
                .match(ClientRead.class, this::onClientReadMsg)

                .match(ClientUpdate.class, twoPhaseCommitDelegate::onClientUpdateMsg)
                .match(ReplicaUpdate.class, twoPhaseCommitDelegate::onReplicaUpdateMsg)
                .match(MasterUpdate.class, twoPhaseCommitDelegate::onMasterUpdateMsg)
                .match(ReplicaUpdateAck.class, twoPhaseCommitDelegate::onReplicaUpdateAckMsg)
                .match(MasterUpdateOk.class, twoPhaseCommitDelegate::onMasterUpdateOkMsg)
                .match(ReplicaNextDead.class, electionDelegate::onReplicaNextDead)

                .match(ReplicaElection.class, electionDelegate::onReplicaElectionMsg)
                .match(MasterSync.class, electionDelegate::onMasterSyncMsg)
                .match(ReplicaElectionAck.class, electionDelegate::onReplicaElectionAckMsg)

                .match(MasterHeartBeat.class, heartbeatDelegate::onMasterHeartBeatMsg)
                .match(MasterTimeout.class, this::onMasterTimeoutMsg)
                .match(HeartBeatReminder.class, heartbeatDelegate::onMasterHeartBeatReminderMsg)
                .build();
    }

    private void onStartMsg(Start msg) {
        this.replicas = msg.replicas;
        this.value = msg.initialValue;
        this.masterId = -1;
        this.heartbeatDelegate.setup();
        electionDelegate.onStartMsg(msg);
    }

    public int getId() {
        return this.id;
    }

    private void onClientReadMsg(ClientRead msg) {
        getSender().tell(new ReplicaReadReply(value), getSender());
    }

    boolean isMaster() {
        return id == masterId;
    }

    void tellMaster(Object message) {
        if (masterId >= 0) {
            latency();
            replicas.get(masterId).tell(message, getSelf());
        }
    }

    void tellBroadcast(Object message) {
        for (ActorRef replica : replicas) {
            latency();
            replica.tell(message, getSelf());
        }
    }

    private void latency () {
        try {
            Random random = ThreadLocalRandom.current();
            Thread.sleep(random.nextInt(10) + 1);
        } catch (InterruptedException e) { }
    }

    void logMessageIgnored(String reason) {
        log("ignored message: " + reason);
    }

    TimeoutManager getTimeoutManager() {
        return timeoutManager;
    }

    void setValue(int value) {
        this.value = value;
    }

    void setMasterId(int masterId) {
        this.masterId = masterId;
    }

    public List<ActorRef> getReplicas() {
        return replicas;
    }

    public Timestamp getLatestUpdate() {
        return latestUpdate;
    }

    public void setLatestUpdate(Timestamp latestUpdate) {
        this.latestUpdate = latestUpdate;
    }

    void onMasterTimeoutMsg(MasterTimeout msg) {
        heartbeatDelegate.startElection();
        electionDelegate.onMasterTimeoutMsg(msg);
    }
    void endElection() {
        heartbeatDelegate.endElection();
    }

    void cancelHeartbeat(){
        heartbeatDelegate.cancelHeartbeat();
    }
}
