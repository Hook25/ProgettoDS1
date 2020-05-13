package it.unitn.ds1.project;

import scala.Int;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Messages {

    public static class ClientUpdate implements Serializable {
        final int value;

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
        final int value;

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
        final int value;

        final Timestamp timestamp;

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
        final Timestamp timestamp;

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
        final Timestamp timestamp;

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
        final Map<Int, List<Timestamp>> historyByNodeId;

        public ReplicaElection(Map<Int, List<Timestamp>> historyByNodeId) {
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
        final List<Timestamp> history;
        final Int masterId;

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
        final int value;

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
