package it.unitn.ds1.project;

import akka.actor.AbstractActor.Receive;

import it.unitn.ds1.project.actors.ReplicaActor;

import java.util.function.Function;

public class Crasher {
    Timestamp crashTime;
    ReplicaActor replica;
    Function<Object, Boolean> instanceF;
    Boolean crashed;
    Receive receiver;
    public Crasher(ReplicaActor replica){
        this.replica = replica;
        crashed = false;
    }
    public void setReceiver(Receive receiver){
        this.receiver = receiver;
    }
    public void setTimestamp(Timestamp ts){
        this.crashTime = ts;
    }
    public void setInstanceF(Function<Object, Boolean> instanceF){
        this.instanceF = instanceF;
    }
    public void consume(Object message){
        if(crashed){ return; }
        if(instanceF == null || crashTime == null){
            receiver.onMessage().apply(message);
        }else if(instanceF.apply(message) && replica.getLatestTimestamp().equals(crashTime)){
            System.out.println("NOOOOOOOOOOOOOOOOOOOO");
            crashed = true;
        }else{
            receiver.onMessage().apply(message);
        }

    }
}
