package it.unitn.ds1.project.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Crasher;
import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.TimeoutManager;
import it.unitn.ds1.project.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class ReplicaActor extends ActorWithId {

    private int value;

    private List<ActorRef> replicas;

    private int masterId;

    private final int id;


    private final List<Timestamp> updateHistory = new ArrayList<>();

    private final TimeoutManager timeoutManager = new TimeoutManager(getSelf(), getContext().system());

    private final TwoPhaseCommitDelegate twoPhaseCommitDelegate = new TwoPhaseCommitDelegate(this);
    private final ElectionDelegate electionDelegate = new ElectionDelegate(this);
    private final HeartbeatDelegate heartbeatDelegate = new HeartbeatDelegate(this);

    private Crasher crashHandler = new Crasher(this);

    /**
     * used only by the master
     */
    private Timestamp latestTimestamp = new Timestamp(0, 0);

    static public Props props(int id) {
        return Props.create(ReplicaActor.class, () -> new ReplicaActor(id));
    }

    public ReplicaActor(int id) {
        super("Replica", id);
        this.id = id;
    }
    public void onCrashPlannerMsg(CrashPlanner msg){
        this.crashHandler.setTimestamp(msg.ts);
        this.crashHandler.setInstanceF(msg.instanceF);
    }
    @Override
    public Receive createReceive() {
        Receive rc = receiveBuilder()
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
                .match(CrashPlanner.class, this::onCrashPlannerMsg)
                .build();
        crashHandler.setReceiver(rc);
        return receiveBuilder().matchAny(crashHandler::consume).build();
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

    List<Timestamp> getUpdateHistory() {
        return updateHistory;
    }

    void setUpdateHistory(List<Timestamp> history) {
        updateHistory.clear();
        updateHistory.addAll(history);
    }

    void setMasterId(int masterId) {
        this.masterId = masterId;
    }

    public List<ActorRef> getReplicas() {
        return replicas;
    }

    public Timestamp getLatestTimestamp() {
        return latestTimestamp;
    }

    public void setLatestTimestamp(Timestamp latestTimestamp) {
        this.latestTimestamp = latestTimestamp;
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
