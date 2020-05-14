package it.unitn.ds1.project;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class MasterTimeoutManager {

    private static final int MASTER_UPDATE_TIMEOUT = 1000;
    private static final int MASTER_UPDATE_OK_TIMEOUT = 1000;
    private static final int HEART_BEAT_TIMEOUT = 10000;

    private final ActorRef replica;
    private final ActorSystem system;

    private Cancellable masterUpdateMsgTimeout;
    private Cancellable masterUpdateOkMsgTimeout;
    private Cancellable heartBeatMsgTimeout;

    public MasterTimeoutManager(ActorRef replica, ActorSystem system) {
        this.replica = replica;
        this.system = system;
        onMasterHeartBeatMsg();
    }


    public void resetMasterUpdateMsgTimeout() {
        // TODO: do we need to use a Map here?
        cancel(masterUpdateMsgTimeout);
        masterUpdateMsgTimeout = setTimeout(MASTER_UPDATE_TIMEOUT);
    }

    public void onMasterUpdateMsg() {
        cancel(masterUpdateMsgTimeout);
    }

    public void resetMasterUpdateOkMsgTimeout() {
        // TODO: do we need to use a Map here?
        cancel(masterUpdateOkMsgTimeout);
        masterUpdateOkMsgTimeout = setTimeout(MASTER_UPDATE_OK_TIMEOUT);
    }

    public void onMasterUpdateOkMsg() {
        cancel(masterUpdateOkMsgTimeout);
    }

    public void onMasterHeartBeatMsg() {
        cancel(heartBeatMsgTimeout);
        heartBeatMsgTimeout = setTimeout(HEART_BEAT_TIMEOUT);
    }

    private void cancel(Cancellable toCancel) {
        if (toCancel != null && !toCancel.isCancelled()) {
            toCancel.cancel();
        }
    }

    private Cancellable setTimeout(int time) {
        return system.scheduler().scheduleOnce(
                Duration.create(time, TimeUnit.MILLISECONDS),
                replica,
                new Messages.MasterTimeout(),
                system.dispatcher(),
                replica
        );
    }
}
