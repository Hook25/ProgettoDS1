package it.unitn.ds1.project;

import akka.actor.ActorSystem;

public class Main {
    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("quorumTotalOrder");
        System.out.println("Hello world");
    }
}
