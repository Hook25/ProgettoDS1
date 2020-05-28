package it.unitn.ds1.project.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.managers.CrashManager;
import it.unitn.ds1.project.models.Messages.*;
import it.unitn.ds1.project.managers.TimeoutManager;
import it.unitn.ds1.project.models.Timestamp;
import it.unitn.ds1.project.models.Update;
import it.unitn.ds1.project.delegates.ElectionDelegate;
import it.unitn.ds1.project.delegates.HeartbeatDelegate;
import it.unitn.ds1.project.delegates.TwoPhaseCommitDelegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ReplicaActor extends ActorWithId {

    private List<Update> history = Update.initialList();

    private List<ActorRef> replicas;

    private int masterId;

    private final int id;

    private final TimeoutManager timeoutManager = new TimeoutManager(getSelf(), getContext().system());

    private final TwoPhaseCommitDelegate twoPhaseCommitDelegate = new TwoPhaseCommitDelegate(this);
    private final ElectionDelegate electionDelegate = new ElectionDelegate(this);
    private final HeartbeatDelegate heartbeatDelegate = new HeartbeatDelegate(this);

    private final CrashManager crashHandler = new CrashManager(this);

    static public Props props(int id) {
        return Props.create(ReplicaActor.class, () -> new ReplicaActor(id));
    }

    public ReplicaActor(int id) {
        super("Replica", id);
        this.id = id;
    }

    public void onCrashPlanMsg(CrashPlan msg) {
        this.crashHandler.setTimestamp(msg.ts);
        this.crashHandler.setCrashCriteria(msg.crashCriteria);
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
                .match(CrashPlan.class, this::onCrashPlanMsg)
                .build();
        crashHandler.setReceiver(rc);
        return receiveBuilder().matchAny(crashHandler::consume).build();
    }

    private void onStartMsg(Start msg) {
        this.replicas = msg.replicas;
        this.masterId = -1;
        this.heartbeatDelegate.setup();
        electionDelegate.onStartMsg(msg);
    }

    public int getId() {
        return this.id;
    }

    private void onClientReadMsg(ClientRead msg) {
        getSender().tell(new ReplicaReadReply(getLatestUpdate().value), getSender());
    }

    public boolean isMaster() {
        return id == masterId;
    }

    public void tellMaster(Object message) {
        if (masterId >= 0) {
            latency();
            replicas.get(masterId).tell(message, getSelf());
        }
    }

    public void tellBroadcast(Object message) {
        for (ActorRef replica : replicas) {
            latency();
            replica.tell(message, getSelf());
        }
    }

    private void latency() {
        try {
            Random random = ThreadLocalRandom.current();
            Thread.sleep(random.nextInt(10) + 1);
        } catch (InterruptedException e) {
        }
    }

    public void logMessageIgnored(String reason) {
        log("ignored message: " + reason);
    }

    public TimeoutManager getTimeoutManager() {
        return timeoutManager;
    }

    public void updateValue(Update update) {
        history.add(update);
    }

    public void setMasterId(int masterId) {
        this.masterId = masterId;
    }

    public List<ActorRef> getReplicas() {
        return replicas;
    }

    public Update getLatestUpdate() {
        if(history.isEmpty()) {
            throw new IllegalStateException("history is empty");
        }
        return history.get(history.size() - 1);
    }

    void onMasterTimeoutMsg(MasterTimeout msg) {
        heartbeatDelegate.startElection();
        electionDelegate.onMasterTimeoutMsg(msg);
    }

    public void endElection() {
        heartbeatDelegate.endElection();
    }

    public void cancelHeartbeat() {
        heartbeatDelegate.cancelHeartbeat();
    }

    public void setMasterTimestamp(Timestamp timestamp) {
        twoPhaseCommitDelegate.setMasterTimestamp(timestamp);
    }

    public HeartbeatDelegate getHeartbeatDelegate() {
        return heartbeatDelegate;
    }

    public List<Update> getHistory() {
        return history;
    }

    public void setHistory(List<Update> history) {
        this.history = new ArrayList<>(history);
    }

    public int getMasterId() {
        return masterId;
    }
}
