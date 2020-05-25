package it.unitn.ds1.project.actors;

import it.unitn.ds1.project.Messages;
import it.unitn.ds1.project.TimeoutManager;

public class HeartbeatDelegate {

    private static final int HEARTBEAT_RATE_MS = 50;
    private static final int HEARTBEAT_TIMEOUT_T = 3; //timeout after HEARTBEAT_TIMEOUT_T * HEARTBEAT_RATE_S
    public static final int HEARTBEAT_TIMEOUT_MS = HEARTBEAT_RATE_MS * HEARTBEAT_TIMEOUT_T;

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

    void onMasterHeartBeatMsg(Messages.MasterHeartBeat msg) {
        timeoutManager.cancelTimeout(msg);
        setupTimeoutNextHeartBeat();
    }
    void endElection(){
        this.setupHeartBeat();
    }

    void setup(){
        this.setupHeartBeat();
    }

    void startElection(){
        timeoutManager.cancelTimeout(new Messages.MasterHeartBeat());
    }
    void onMasterHeartBeatReminderMsg(Messages.HeartBeatReminder msg) {
        replica.tellBroadcast(new Messages.MasterHeartBeat());
    }
    void cancelHeartbeat(){
        timeoutManager.cancelTimeout(new Messages.MasterHeartBeat());
    }
}
