package it.unitn.ds1.project.delegates;

import it.unitn.ds1.project.models.Messages;
import it.unitn.ds1.project.managers.TimeoutManager;
import it.unitn.ds1.project.actors.ReplicaActor;

import static it.unitn.ds1.project.managers.Timeout.HEARTBEAT_RATE_MS;
import static it.unitn.ds1.project.managers.Timeout.HEARTBEAT_TIMEOUT_MS;

public class HeartbeatDelegate {
    private final ReplicaActor replica;
    private final TimeoutManager timeoutManager;

    public HeartbeatDelegate(ReplicaActor replica) {
        this.replica = replica;
        this.timeoutManager = replica.getTimeoutManager();
    }

    private void setupHeartBeat() {
        if (replica.isMaster()) {
            setupMasterHeartBeat();
        } else {
            setupTimeoutNextHeartBeat();
        }
    }

    private void setupMasterHeartBeat() {
        TimeoutManager.scheduleAtFixedRate(replica, HEARTBEAT_RATE_MS, new Messages.HeartBeatReminder());
    }

    private void setupTimeoutNextHeartBeat() {
        Messages.AcknowledgeableMessage<Messages.MessageId> waitHeartBeat = new Messages.AcknowledgeableMessage<Messages.MessageId>(Messages.StringMessageId.heartbeat()) {
        };
        timeoutManager.startTimeout(waitHeartBeat, HEARTBEAT_TIMEOUT_MS, new Messages.MasterTimeout());
    }

    public void onMasterHeartBeatMsg(Messages.MasterHeartBeat msg) {
        postponeHeartBeatTimeout();
    }

    public void endElection() {
        this.setupHeartBeat();
    }

    public void setup() {
        this.setupHeartBeat();
    }

    public void startElection() {
        timeoutManager.cancelTimeout(new Messages.MasterHeartBeat());
    }

    public void onMasterHeartBeatReminderMsg(Messages.HeartBeatReminder msg) {
        replica.tellBroadcast(new Messages.MasterHeartBeat());
    }

    public void cancelHeartbeat() {
        timeoutManager.cancelTimeout(new Messages.MasterHeartBeat());
    }

    public void postponeHeartBeatTimeout() {
        cancelHeartbeat();
        setupTimeoutNextHeartBeat();
    }
}
