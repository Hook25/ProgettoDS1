package it.unitn.ds1.project.actors;

import it.unitn.ds1.project.Messages.*;
import it.unitn.ds1.project.TimeoutManager;
import it.unitn.ds1.project.Timestamp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ElectionDelegate {

    private static final int ELECTION_ACK_TIMEOUT_MS = 400;

    private final TimeoutManager timeoutManager;
    private final ReplicaActor replica;

    public ElectionDelegate(ReplicaActor replica) {
        this.replica = replica;
        timeoutManager = replica.getTimeoutManager();
    }

    void onMasterTimeoutMsg(MasterTimeout msg) {
        startElection(new HashMap<>());
    }

    void startElection(Map<Integer, List<Timestamp>> partial) {
        partial.put(replica.getId(), replica.getUpdateHistory());
        ReplicaElection toSend = new ReplicaElection(partial);
        replica.tellNext(toSend);
        this.timeoutManager.startTimeout(toSend, ELECTION_ACK_TIMEOUT_MS, new ReplicaNextDead(partial));
    }

    void onReplicaElectionAckMsg(ReplicaElectionAck msg) {
        timeoutManager.cancelTimeout(msg);
    }

    void onReplicaElectionMsg(ReplicaElection msg) {
        replica.getSender().tell(new ReplicaElectionAck(msg.id), replica.getSelf());
        if (msg.historyByNodeId.containsKey(replica.getId())) { //full ring trip done
            this.pickLeader(msg.historyByNodeId);
        } else {
            this.startElection(msg.historyByNodeId);
        }
    }

    int getNewBest(Map<Integer, List<Timestamp>> lts) {
        int best = -1;
        Optional<Timestamp> ts =
                lts.entrySet().stream().flatMap(entry -> entry.getValue().stream()).max(Timestamp.COMPARATOR);
        for (Map.Entry<Integer, List<Timestamp>> i_ts : lts.entrySet()) {
            if (best < 0 && i_ts.getValue().contains(ts)) {
                best = i_ts.getKey();
            }
        }
        return best;
    }

    void pickLeader(Map<Integer, List<Timestamp>> lts) {
        int new_leader = getNewBest(lts);
        if (replica.getId() == new_leader) {
            replica.tellBroadcast(new MasterSync(replica.getUpdateHistory(), new_leader));
        }

        /*if(replica.GetId() == 0){
            System.out.println("Done election");
        }
        replica.setMasterId(0);
        replica.SetupHB();*/
    }


    void onReplicaNextDead(ReplicaNextDead msg) {
        replica.bumpNext();
        startElection(msg.partial);
        System.out.println("Next is dead");
    }

    void onMasterSyncMsg(MasterSync msg) {
        replica.setUpdateHistory(msg.history);
        replica.setMasterId(msg.masterId);
        replica.setupHeartBeat();
    }

}
