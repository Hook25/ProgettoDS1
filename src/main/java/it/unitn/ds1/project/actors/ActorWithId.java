package it.unitn.ds1.project.actors;

import akka.actor.AbstractActor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public abstract class ActorWithId extends AbstractActor {

    private final static String LOG_FOLDER = "/tmp/logs";

    private final PrintWriter logFile;

    private final String actorType;

    protected final int id;

    public ActorWithId(String actorType, int id) {
        this.actorType = actorType;
        this.id = id;
        try {
            logFile = new PrintWriter(new File(String.format("%s/%s-%d.txt", LOG_FOLDER, actorType, id)));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("cannot create log file", e);
        }
    }

    public void log(String message) {
        message = String.format("%s %2d %s", actorType, id, message);
        System.out.println(message);
        logFile.println(message);
        logFile.flush();
    }



}
