package it.unitn.ds1.project.managers;

public abstract class Timeout {
    public static final int ELECTION_ACK_TIMEOUT_MS = 100;

    public static final int HEARTBEAT_RATE_MS = 600;
    public static final int HEARTBEAT_TIMEOUT_T = 3; //timeout after HEARTBEAT_TIMEOUT_T * HEARTBEAT_RATE_S
    public static final int HEARTBEAT_TIMEOUT_MS = HEARTBEAT_RATE_MS * HEARTBEAT_TIMEOUT_T;

    public static final int MASTER_UPDATE_TIMEOUT = 100;
    public static final int MASTER_UPDATE_OK_TIMEOUT = 100;
}