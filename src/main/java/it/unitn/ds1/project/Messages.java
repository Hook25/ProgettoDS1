package it.unitn.ds1.project;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.sql.Time;
import java.util.*;
import java.util.function.Function;

public class Messages {

    public interface MessageId extends Serializable {
    }


    public static abstract class AcknowledgeableMessage<T extends MessageId> implements Serializable {
        public final T id;

        public AcknowledgeableMessage(T id) {
            this.id = id;
        }
    }

    public static abstract class Ack<T extends MessageId> implements Serializable {
        public final T acknowledgedId;

        private Ack(T acknowledgedId) {
            this.acknowledgedId = acknowledgedId;
        }
    }

    public static class Start implements Serializable {
        public final List<ActorRef> replicas;
        public final int initialValue;

        public Start(List<ActorRef> replicas, int initialValue) {
            this.replicas = replicas;
            this.initialValue = initialValue;
        }

        @Override
        public String toString() {
            return "Start{" +
                    "replicas=" + replicas +
                    ", initialValue=" + initialValue +
                    '}';
        }
    }

    public static class MasterTimeout implements Serializable {

    }

    public static class CrashPlanner implements  Serializable{
        public Timestamp ts;
        public Function<Object, Boolean> instanceF;
        public  CrashPlanner(Timestamp ts, Function<Object, Boolean> instanceF){
            this.ts = ts;
            this.instanceF = instanceF;
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

    public static class ReplicaUpdate extends AcknowledgeableMessage<StringMessageId> implements Serializable {

        public final int value;

        private ReplicaUpdate(int value) {
            super(new StringMessageId());
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

    public static class MasterUpdate extends Ack<StringMessageId> implements Serializable {

        public final int value;

        public final Timestamp timestamp;

        private MasterUpdate(StringMessageId acknowledgedId, int value, Timestamp timestamp) {
            super(acknowledgedId);
            this.value = value;
            this.timestamp = timestamp;
        }

        public static Object fromReplicaUpdate(ReplicaUpdate msg, Timestamp t) {
            return new MasterUpdate(msg.id, msg.value, t);
        }

        @Override
        public String toString() {
            return "MasterUpdate{" +
                    "value=" + value +
                    '}';
        }
    }

    public static class ReplicaUpdateAck extends AcknowledgeableMessage<Timestamp> implements Serializable {
        public final Timestamp timestamp;

        private ReplicaUpdateAck(Timestamp timestamp) {
            super(timestamp);
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

    public static class MasterUpdateOk extends Ack<Timestamp> implements Serializable {

        public final Timestamp timestamp;

        public MasterUpdateOk(Timestamp timestamp) {
            super(timestamp);
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "MasterUpdateOk{" +
                    "timestamp=" + timestamp +
                    '}';
        }
    }

    public static class MasterHeartBeat extends Ack<StringMessageId> implements Serializable {
        public MasterHeartBeat() {
            super(StringMessageId.heartbeat());
        }

        @Override
        public String toString() {
            return "MasterHeartBeat";
        }
    }

    public static class ReplicaElection extends AcknowledgeableMessage<StringMessageId> implements Serializable {
        public final Map<Integer, List<Timestamp>> historyByNodeId;

        public ReplicaElection(Map<Integer, List<Timestamp>> historyByNodeId) {
            super(new StringMessageId());
            this.historyByNodeId = Collections.unmodifiableMap(new HashMap<>(historyByNodeId));
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
            this.history = Collections.unmodifiableList(new ArrayList<>(history));
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

    public static class ReplicaElectionAck extends Ack<StringMessageId>  implements Serializable {
        public  ReplicaElectionAck(StringMessageId id){
            super(id);
        }
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReplicaReadReply that = (ReplicaReadReply) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "ReplicaReadReply{" +
                    "value=" + value +
                    '}';
        }
    }

    public static class HeartBeatReminder implements Serializable {
    }

    public static class StringMessageId implements MessageId {
        private final String value;

        public StringMessageId() {
            value = UUID.randomUUID().toString();
        }

        public StringMessageId(String value) {
            this.value = value;
        }

        public static StringMessageId heartbeat() {
            return new StringMessageId("heartbeat");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StringMessageId that = (StringMessageId) o;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class ReplicaCheckMasterDead implements Serializable {
        @Override
        public String toString(){
            return "ReplicaCheckMasterDead{}";
        }
    }

    public static class ReplicaNextDead extends AcknowledgeableMessage<StringMessageId> implements Serializable{
        public final Map<Integer, List<Timestamp>> partial;

        public ReplicaNextDead(Map<Integer, List<Timestamp>> partial){
            super(new StringMessageId());
            this.partial = partial;
        }
        @Override
        public String toString() {
            return "ReplicaNextDead{}";
        }
    }
}
