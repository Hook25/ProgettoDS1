package it.unitn.ds1.project.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.Messages;
import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.TimeoutManager;
import it.unitn.ds1.project.Timestamp;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReplicaActor extends ActorWithId {

    private static final int HEART_BEAT = 5000;

    private int value;

    private List<ActorRef> replicas;

    private int masterId;

    private final List<Timestamp> updateHistory = new ArrayList<>();

    private final TimeoutManager timeoutManager = new TimeoutManager(getSelf(), getContext().system());

    private final TwoPhaseCommitDelegate twoPhaseCommitDelegate = new TwoPhaseCommitDelegate(this);

    static public Props props(int id) {
        return Props.create(ReplicaActor.class, () -> new ReplicaActor(id));
    }

    public ReplicaActor(int id) {
        super("Replica", id);
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
                .build();
    }

    private void onStartMsg(Start msg) {
        this.replicas = msg.replicas;
        this.value = msg.initialValue;
        this.masterId = msg.masterId;
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
        log("I think master has crashed");
        // TODO: Start election
    }

    private void onMasterHeartBeatMsg(MasterHeartBeat msg) {
        timeoutManager.cancelTimeout(msg);
        setupTimeoutNextHeartBeat();
    }

    private void setupTimeoutNextHeartBeat () {
        timeoutManager.startTimeout(new AcknowledgeableMessage<MessageId>(StringMessageId.heartbeat()) {}, 10000, new MasterTimeout());
    }

    private void onReplicaElectionMsg(ReplicaElection msg) {

    }

    private void onMasterSyncMsg(MasterSync msg) {

    }

    private void onReplicaElectionAckMsg(ReplicaElectionAck msg) {

    }

    private void onClientReadMsg(ClientRead msg) {
        getSender().tell(new ReplicaReadReply(value), getSender());
    }

    private void onMasterHeartBeatReminderMsg(HeartBeatReminder msg) {
        tellBroadcast(new MasterHeartBeat());
    }

    private void startMasterHeartBeat() {
        context().system().scheduler().scheduleAtFixedRate(
                Duration.create(HEART_BEAT, TimeUnit.MILLISECONDS),
                Duration.create(HEART_BEAT, TimeUnit.MILLISECONDS),
                getSelf(),
                new Messages.HeartBeatReminder(),
                context().system().dispatcher(),
                getSelf()
        );
    }

    boolean amIMaster() {
        return id == masterId;
    }

    void tellMaster(Object message) {
        replicas.get(masterId).tell(message, getSelf());
    }

    void tellBroadcast(Object message) {
        for (ActorRef replica : replicas) {
            replica.tell(message, getSelf());
        }
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

    public List<ActorRef> getReplicas() {
        return replicas;
    }
}
