package it.unitn.ds1.project;

import akka.actor.ActorRef;
import scala.Int;

import java.io.Serializable;
import java.io.SerializablePermission;
import java.util.List;
import java.util.Map;

public class Messages {

    public static class Start implements Serializable {
        public final List<ActorRef> replicas;
        public final int masterId;
        public final int initialValue;

        public Start(List<ActorRef> replicas, int masterId, int initialValue) {
            this.replicas = replicas;
            this.masterId = masterId;
            this.initialValue = initialValue;
        }

        @Override
        public String toString() {
            return "Start{" +
                    "replicas=" + replicas +
                    ", masterId=" + masterId +
                    ", initialValue=" + initialValue +
                    '}';
        }
    }

    public static class ClientUpdate implements Serializable {
        public final int value;

        public ClientUpdate(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "ClientUpdate{" +
                    "value=" + value +
                    '}';
        }
    }

    public static class ReplicaUpdate implements Serializable {
        public final int value;

        public ReplicaUpdate(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "ReplicaUpdate{" +
                    "value=" + value +
                    '}';
        }
    }

    public static class MasterUpdate implements Serializable {
        public final int value;

        public final Timestamp timestamp;

        public MasterUpdate(int value, Timestamp timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "MasterUpdate{" +
                    "value=" + value +
                    '}';
        }
    }

    public static class ReplicaUpdateAck implements Serializable {
        public final Timestamp timestamp;

        public ReplicaUpdateAck(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "ReplicaUpdateAck{" +
                    "timestamp=" + timestamp +
                    '}';
        }
    }

    public static class MasterUpdateOk implements Serializable {
        public final Timestamp timestamp;

        public MasterUpdateOk(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "MasterUpdateOk{" +
                    "timestamp=" + timestamp +
                    '}';
        }
    }

    public static class MasterHeartBeat implements Serializable {
        @Override
        public String toString() {
            return "MasterHeartBeat";
        }
    }

    public static class ReplicaElection implements Serializable {
        public final Map<Integer, List<Timestamp>> historyByNodeId;

        public ReplicaElection(Map<Integer, List<Timestamp>> historyByNodeId) {
            this.historyByNodeId = historyByNodeId;
        }


        @Override
        public String toString() {
            return "ReplicaElection{" +
                    "historyByNodeId=" + historyByNodeId +
                    '}';
        }
    }

    public static class MasterSync implements Serializable {
        public final List<Timestamp> history;
        public final int masterId;

        public MasterSync(List<Timestamp> history, int masterId) {
            this.history = history;
            this.masterId = masterId;
        }

        @Override
        public String toString() {
            return "MasterSync{" +
                    "history=" + history +
                    ", masterId=" + masterId +
                    '}';
        }
    }

    public static class ReplicaElectionAck implements Serializable {
        @Override
        public String toString() {
            return "ReplicaElectionAck";
        }
    }

    public static class ClientRead implements Serializable {
        @Override
        public String toString() {
            return "ClientRead";
        }
    }

    public static class ReplicaReadReply implements Serializable {
        public final int value;

        public ReplicaReadReply(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "ReplicaReadReply{" +
                    "value=" + value +
                    '}';
        }
    }
    public static class ReplicaCheckMasterDead implements Serializable {
        @Override
        public String toString(){
            return "ReplicaCheckMasterDead{}";
        }
    }

    public static class ReplicaNextDead implements Serializable{
        public final Map<Integer, List<Timestamp>> partial;

        public ReplicaNextDead(Map<Integer, List<Timestamp>> partial){
            this.partial = partial;
        }
        @Override
        public String toString() {
            return "ReplicaNextDead{}";
        }
    }

}
