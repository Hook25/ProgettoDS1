package it.unitn.ds1.project;

import akka.actor.ActorRef;
import scala.Int;

import java.io.Serializable;
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

        private ReplicaUpdate(int value) {
            this.value = value;
        }

        public static ReplicaUpdate fromClientUpdate(ClientUpdate msg) {
            return new ReplicaUpdate(msg.value);
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

        private MasterUpdate(int value, Timestamp timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public static Object fromReplicaUpdate(ReplicaUpdate msg, Timestamp t) {
            return new MasterUpdate(msg.value, t);
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

        private ReplicaUpdateAck(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        public static ReplicaUpdateAck fromMasterUpdate(MasterUpdate msg) {
            return new ReplicaUpdateAck(msg.timestamp);
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
        public final Int masterId;

        public MasterSync(List<Timestamp> history, Int masterId) {
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
}
