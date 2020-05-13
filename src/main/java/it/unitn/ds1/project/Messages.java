package it.unitn.ds1.project;

import java.io.Serializable;

public class Messages {


    public static class ClientUpdate implements Serializable {
        final int value;

        public ClientUpdate(int value) {
            this.value = value;
        }
    }

    public static class ReplicaUpdate implements Serializable {
        final int value;

        public ReplicaUpdate(int value) {
            this.value = value;
        }
    }

    public static class MasterUpdate implements Serializable {
        final int value;

        public MasterUpdate(int value) {
            this.value = value;
        }
    }

    public static class MasterUpdateOk implements Serializable {

    }

    public static class MasterHeartBeet implements Serializable {

    }

    public static class ReplicaElection implements Serializable {

    }

    public static class ReplicaAck implements Serializable {

    }

    public static class MasterSync implements Serializable {

    }

    public static class ReplicaVote implements Serializable {

    }

    public static class ClientRead implements Serializable {

    }
}
