package it.unitn.ds1.project.models;

import akka.actor.ActorRef;
import it.unitn.ds1.project.actors.ReplicaActor;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
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

        public Start(List<ActorRef> replicas) {
            this.replicas = replicas;
        }

        @Override
        public String toString() {
            return "Start{" +
                    "replicas=" + replicas +
                    '}';
        }
    }

    public static class MasterTimeout implements Serializable {

    }

    public static class CrashPlan implements Serializable {
        public final Timestamp ts;
        public final BiFunction<ReplicaActor, Object, Boolean> crashCriteria;

        public CrashPlan(Timestamp ts, BiFunction<ReplicaActor, Object, Boolean> crashCriteria) {
            this.ts = ts;
            this.crashCriteria = crashCriteria;
        }

        public CrashPlan(Timestamp ts, Function<Object, Boolean> crashCriteria) {
            this(ts, (receiver, msg) -> crashCriteria.apply(msg));
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

        public ReplicaUpdate(int value) {
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

        public final Update update;


        public static MasterUpdate fromReplicaUpdate(ReplicaUpdate msg, Timestamp t) {
            return new MasterUpdate(msg.id, new Update(t, msg.value));
        }

        private MasterUpdate(StringMessageId acknowledgedId, Update update) {
            super(acknowledgedId);
            this.update = update;
        }

        @Override
        public String toString() {
            return "MasterUpdate{" +
                    "update=" + update +
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
            return new ReplicaUpdateAck(msg.update.timestamp);
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MasterUpdateOk that = (MasterUpdateOk) o;
            return timestamp.equals(that.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp);
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
        public final Map<Integer, Timestamp> latestUpdatesByNodeId;

        public ReplicaElection(Map<Integer, Timestamp> latestUpdatesByNodeId) {
            super(new StringMessageId());
            this.latestUpdatesByNodeId = Collections.unmodifiableMap(new HashMap<>(latestUpdatesByNodeId));
        }


        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("Election -> ");
            latestUpdatesByNodeId.keySet().forEach(s::append);
            return s.toString();
        }
    }

    public static class MasterSync implements Serializable {
        public final Timestamp latestUpdate;
        public final int masterId;
        public final List<Update> history;

        public MasterSync(Timestamp latestUpdate, int masterId, List<Update> history) {
            this.latestUpdate = latestUpdate;
            this.masterId = masterId;
            this.history = Collections.unmodifiableList(new ArrayList<>(history));
        }

        @Override
        public String toString() {
            return "MasterSync{" +
                    "history=" + latestUpdate +
                    ", masterId=" + masterId +
                    '}';
        }
    }

    public static class ReplicaElectionAck extends Ack<StringMessageId> implements Serializable {
        public ReplicaElectionAck(StringMessageId id) {
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

    public static class ReplicaNextDead extends AcknowledgeableMessage<StringMessageId> implements Serializable {
        public final Map<Integer, Timestamp> partial;
        public final Integer next;

        public ReplicaNextDead(Map<Integer, Timestamp> partial, int next) {
            super(new StringMessageId());
            this.partial = partial;
            this.next = next;
        }

        @Override
        public String toString() {
            return "ReplicaNextDead{}";
        }
    }

    public static class ReminderClientRead implements Serializable {
        public final int replicaId;

        public ReminderClientRead(int replicaId) {
            this.replicaId = replicaId;
        }
    }

    public static class ReminderClientUpdate implements Serializable {
        public final int replicaId;
        public final int value;

        public ReminderClientUpdate(int replicaId, int value) {
            this.replicaId = replicaId;
            this.value = value;
        }
    }
}
