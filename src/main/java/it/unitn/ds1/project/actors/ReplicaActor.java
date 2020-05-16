package it.unitn.ds1.project.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Messages;
import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.TimeoutManager;
import it.unitn.ds1.project.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ReplicaActor extends ActorWithId {

    private int value;

    private List<ActorRef> replicas;

    private static final int HEARTBEAT_RATE_MS = 200;
    private static final int HEARTBEAT_TIMEOUT_T = 3; //timeout after HEARTBEAT_TIMEOUT_T * HEARTBEAT_RATE_S
    private static final int HEARTBEAT_TIMEOUT_MS = HEARTBEAT_RATE_MS * HEARTBEAT_TIMEOUT_T;

    private int masterId;

    private int next;
    private final int id;


    private final List<Timestamp> updateHistory = new ArrayList<>();

    private final TimeoutManager timeoutManager = new TimeoutManager(getSelf(), getContext().system());

    private final TwoPhaseCommitDelegate twoPhaseCommitDelegate = new TwoPhaseCommitDelegate(this);
    private final ElectionDelegate electionDelegate = new ElectionDelegate(this);

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
                .match(ClientUpdate.class, this::onClientUpdateMsg)
                .match(ReplicaUpdate.class, this::onReplicaUpdateMsg)
                .match(MasterUpdate.class, this::onMasterUpdateMsg)
                .match(ReplicaUpdateAck.class, this::onReplicaUpdateAckMsg)
                .match(MasterUpdateOk.class, this::onMasterUpdateOkMsg)
                .match(MasterHeartBeat.class, this::onMasterHeartBeatMsg)
                .match(ReplicaElection.class, this::onReplicaElectionMsg)
                .match(MasterSync.class, this::onMasterSyncMsg)
                .match(ReplicaElectionAck.class, this::onReplicaElectionAckMsg)
                .match(ClientRead.class, this::onClientReadMsg)
                .match(MasterTimeout.class, this::onMasterTimeoutMsg)
                .match(HeartBeatReminder.class, this::onMasterHeartBeatReminderMsg)
                .match(ReplicaNextDead.class, this::onReplicaNextDead)
                .build();
    }

    private void onStartMsg(Start msg) {
        this.replicas = msg.replicas;
        this.value = msg.initialValue;
        this.masterId = -1;
        setupHeartBeat();
        this.next = this.id;
        this.bumpNext();
    }

    public void setupHeartBeat() {
        if (amIMaster()) {
            startMasterHeartBeat();
        } else {
            setupTimeoutNextHeartBeat();
        }
    }

    private void onClientUpdateMsg(Messages.ClientUpdate msg) {
        twoPhaseCommitDelegate.onClientUpdateMsg(msg);
    }

    private void onReplicaUpdateMsg(Messages.ReplicaUpdate msg) {
        twoPhaseCommitDelegate.onReplicaUpdateMsg(msg);
    }

    private void onMasterUpdateMsg(Messages.MasterUpdate msg) {
        twoPhaseCommitDelegate.onMasterUpdateMsg(msg);
    }

    private void onReplicaUpdateAckMsg(Messages.ReplicaUpdateAck msg) {
        twoPhaseCommitDelegate.onReplicaUpdateAckMsg(msg);
    }

    private void onMasterUpdateOkMsg(Messages.MasterUpdateOk msg) {
        twoPhaseCommitDelegate.onMasterUpdateOkMsg(msg);
    }

    private void onMasterTimeoutMsg(MasterTimeout msg) {
        electionDelegate.onMasterTimeoutMsg(msg);
    }

    private void onMasterHeartBeatMsg(MasterHeartBeat msg) {
        timeoutManager.cancelTimeout(msg);
        setupTimeoutNextHeartBeat();
    }

    private void setupTimeoutNextHeartBeat() {
        AcknowledgeableMessage<MessageId> waitHeartBeat = new AcknowledgeableMessage<MessageId>(StringMessageId.heartbeat()) { };
        timeoutManager.startTimeout(waitHeartBeat, HEARTBEAT_TIMEOUT_MS, new MasterTimeout());
    }

    private void onReplicaNextDead(ReplicaNextDead msg) {
        electionDelegate.onReplicaNextDead(msg);
    }

    public int getNext() {
        return this.next;
    }

    public int getId() {
        return this.id;
    }

    public void bumpNext() {
        this.next = ((this.next + 1) % this.replicas.size());
        if (this.id == this.next) {
            this.bumpNext();
        }
    }

    private void onReplicaElectionMsg(ReplicaElection msg) {
        electionDelegate.onReplicaElectionMsg(msg);
    }

    private void onMasterSyncMsg(MasterSync msg) {
        electionDelegate.onMasterSyncMsg(msg);
    }

    private void onReplicaElectionAckMsg(ReplicaElectionAck msg) {
        electionDelegate.onReplicaElectionAckMsg(msg);
    }

    private void onClientReadMsg(ClientRead msg) {
        getSender().tell(new ReplicaReadReply(value), getSender());
    }

    private void onMasterHeartBeatReminderMsg(HeartBeatReminder msg) {
        tellBroadcast(new MasterHeartBeat());
    }

    private void startMasterHeartBeat() {
        TimeoutManager.scheduleAtFixedRate(this, HEARTBEAT_RATE_MS, new HeartBeatReminder());
    }

    boolean amIMaster() {
        return id == masterId;
    }

    void tellMaster(Object message) {
        if (masterId >= 0) {
            replicas.get(masterId).tell(message, getSelf());
        }
    }

    void tellBroadcast(Object message) {
        for (ActorRef replica : replicas) {
            replica.tell(message, getSelf());
        }
    }

    void tellNext(Object msg) {
        int next = getNext();
        replicas.get(next).tell(msg, getSelf());
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
}
