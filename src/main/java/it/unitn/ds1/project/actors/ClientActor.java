package it.unitn.ds1.project.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.project.models.Messages.ClientUpdate;
import it.unitn.ds1.project.models.Messages.ReminderClientRead;
import it.unitn.ds1.project.models.Messages.ReminderClientUpdate;
import it.unitn.ds1.project.models.Messages.ClientRead;
import it.unitn.ds1.project.models.Messages.ReplicaReadReply;

import java.util.List;

public class ClientActor extends ActorWithId {

    protected final List<ActorRef> replicas;

    static public Props props(int id, List<ActorRef> replicas) {
        return Props.create(ClientActor.class, () -> new ClientActor(id, replicas));
    }

    public ClientActor(int id, List<ActorRef> replicas) {
        super("Client", id);
        this.replicas = replicas;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ReminderClientRead.class, this::readValue)
                .match(ReplicaReadReply.class, this::onReplicaReadReplyMsg)
                .match(ReminderClientUpdate.class, this::updateValue)
                .build();
    }

    private void readValue(ReminderClientRead read) {
        log("read req to " + read.replicaId);
        replicas.get(read.replicaId).tell(new ClientRead(), self());
    }

    private void onReplicaReadReplyMsg(ReplicaReadReply msg) {
        log("read done " + msg.value);
    }

    private void updateValue(ReminderClientUpdate update) {
        replicas.get(update.replicaId).tell(new ClientUpdate(update.value), self());
    }
}
