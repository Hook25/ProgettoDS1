package it.unitn.ds1.project.actors;
import java.io.Serializable;
import java.time.Duration;

import akka.actor.*;
import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.Timestamp;
import scala.concurrent.impl.FutureConvertersImpl;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplicaActor extends AbstractActor {

    private final int id;

    private int value;

    private List<ActorRef> replicas;

    private boolean iAmMaster;

    private static int HEARTBEAT_RATE_S = 200;
    private static int HEARTBEAT_TIMEOUT_T = 3; //timeout after HEARTBEAT_TIMEOUT_T * HEARTBEAT_RATE_S
    private static int ELECTION_ACK_TIMEOUT_S = 6000;

    private static Duration HEARTBEAT_RATE_D = Duration.ofMillis(HEARTBEAT_RATE_S);
    private static Duration HEARTBEAT_TIMEOUT_D = Duration.ofMillis(HEARTBEAT_RATE_S * HEARTBEAT_TIMEOUT_T);
    private static Duration ELECTION_ACK_TIMEOUT_T = Duration.ofMillis(ELECTION_ACK_TIMEOUT_S);

    private int masterId;
    private Instant lastHeartBeat;
    private Cancellable heartBeatTimer; //used to send when master, else for timeout
    private Cancellable electionTimeoutTimer;
    private boolean inElection = false;
    private int ackOnHold = 0;
    private int next;

    private List<Timestamp> history;

    private final ActorSystem system = this.getContext().system();

    static public Props props(int id, boolean master) {
        return Props.create(ReplicaActor.class, () -> new ReplicaActor(id, master));
    }

    public ReplicaActor(int id, boolean iAmMaster) {
        this.id = id;
        this.iAmMaster = iAmMaster;
        this.lastHeartBeat = Instant.now();
        SetupHB();
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
                .match(ReplicaReadReply.class, this::onReplicaReadReplyMsg)
                .match(ReplicaCheckMasterDead.class, this::onReplicaCheckMasterDead)
                .match(ReplicaNextDead.class, this::onReplicaNextDead)
                .build();
    }

    private void onStartMsg(Start msg) {
        this.replicas = msg.replicas;
        this.value = msg.initialValue;
        this.masterId = msg.masterId;
        this.next = this.id;
        this.bumpNext();
    }

    private void SetupHB(){
        if(this.iAmMaster) {
            this.heartBeatTimer = system.scheduler().scheduleWithFixedDelay(
                    HEARTBEAT_RATE_D,
                    HEARTBEAT_RATE_D,
                    getSelf(),
                    new MasterHeartBeat(),
                    system.dispatcher(),
                    getSelf()
            ) ;
        }else{
            this.heartBeatTimer = system.scheduler().scheduleAtFixedRate(
                    HEARTBEAT_TIMEOUT_D,
                    HEARTBEAT_TIMEOUT_D,
                    getSelf(),
                    new ReplicaCheckMasterDead(),
                    system.dispatcher(),
                    getSelf()
            );
        }
    }

    private void onClientUpdateMsg(ClientUpdate msg) {

    }

    private void onReplicaUpdateMsg(ReplicaUpdate msg) {

    }

    private void onMasterUpdateMsg(MasterUpdate msg) {

    }

    private void onReplicaUpdateAckMsg(ReplicaUpdateAck msg) {

    }

    private void onMasterUpdateOkMsg(MasterUpdateOk msg) {

    }

    private void onMasterHeartBeatMsg(MasterHeartBeat msg) {
        if(iAmMaster){
            for(int i = 0; i < replicas.size(); i++){
                if(i!=id){
                    replicas.get(i).tell(new MasterHeartBeat(), getSelf());
                }
            }
        }else{
            this.lastHeartBeat = Instant.now();
        }
    }

    private void onReplicaNextDead(ReplicaNextDead msg){
        if(ackOnHold != 0) {
            ackOnHold --;
            System.out.println("Next is dead");
            bumpNext();
            startElection(msg.partial);
        }
    }

    private int getNext(){
        return this.next;
    }
    private void bumpNext(){
        this.next = ((this.next + 1) % this.replicas.size());
        if(this.id == this.next){ this.bumpNext(); }
    }

    private void startElection(Map<Integer, List<Timestamp>> partial){
        int next = this.getNext();
        this.inElection = true;
        partial.put(this.id, this.history);
        this.replicas.get(next).tell(new ReplicaElection(partial), getSelf());
        this.ackOnHold ++;
        this.electionTimeoutTimer = system.scheduler().scheduleOnce(
                ELECTION_ACK_TIMEOUT_T,
                getSelf(),
                new ReplicaNextDead(partial),
                system.dispatcher(),
                getSelf()
        );
    }

    private void onReplicaCheckMasterDead(ReplicaCheckMasterDead msg) {
        Instant now = Instant.now();
        Duration delta = Duration.between(this.lastHeartBeat, now);
        if(delta.compareTo(HEARTBEAT_TIMEOUT_D) > 0){
            this.heartBeatTimer.cancel();
            this.startElection(new HashMap<>());
        }

    }
    private void pickLeader(Map<Integer, List<Timestamp>> lts){

    }

    private void onReplicaElectionMsg(ReplicaElection msg) {
        if(!this.inElection){
            this.heartBeatTimer.cancel();
        }
        getSender().tell(new ReplicaElectionAck(), getSelf());
        if(msg.historyByNodeId.containsKey(this.id)){ //full ring trip done
            this.pickLeader(msg.historyByNodeId);
        }else{
            this.startElection(msg.historyByNodeId);
        }
    }

    private void onMasterSyncMsg(MasterSync msg) {

    }

    private void onReplicaElectionAckMsg(ReplicaElectionAck msg) {
        this.ackOnHold --;
    }

    private void onClientReadMsg(ClientRead msg) {

    }

    private void onReplicaReadReplyMsg(ReplicaReadReply msg) {

    }


}
