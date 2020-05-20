package it.unitn.ds1.project;

import akka.actor.AbstractActor.Receive;

import it.unitn.ds1.project.actors.ReplicaActor;

import java.util.function.Function;

public class Crasher {
    private Timestamp crashTime;
    private final ReplicaActor replica;
    private Function<Object, Boolean> crashCriteria;
    private boolean crashed = false;
    private Receive receiver;
    public Crasher(ReplicaActor replica){
        this.replica = replica;
    }
    public void setReceiver(Receive receiver){
        this.receiver = receiver;
    }
    public void setTimestamp(Timestamp ts){
        this.crashTime = ts;
    }
    public void setCrashCriteria(Function<Object, Boolean> crashCriteria){
        this.crashCriteria = crashCriteria;
    }
    public void consume(Object message){
        if(crashed){ System.out.println("ignored message, I'm dead"); }
        if(crashCriteria == null || crashTime == null){
            receiver.onMessage().apply(message);
        }else if(crashCriteria.apply(message) && replica.getLatestTimestamp().equals(crashTime)){
            crashed = true;
        }else{
            receiver.onMessage().apply(message);
        }

    }
}
