package it.unitn.ds1.project;

import it.unitn.ds1.project.Messages.MasterSync;
import it.unitn.ds1.project.Messages.ReplicaElection;
import it.unitn.ds1.project.Messages.ReplicaElectionAck;
import org.junit.jupiter.api.Test;

import java.time.Duration;
public class TestElection extends MyAkkaTest {

    @Test
    public void testInitialElection() {
        new MyTestKit(3) {
            {
                sniffer.sendStartMsgFirstScattered();
                within(Duration.ofSeconds(5), () -> {
                    // 0 -> 1
                    sniffer.expectMsgFrom(replicas[1], ReplicaElection.class, replicas[0]);
                    sniffer.expectMsgFrom(replicas[0], ReplicaElectionAck.class, replicas[1]);

                    // 1 -> 2
                    sniffer.expectMsgFrom(replicas[2], ReplicaElection.class, replicas[1]);
                    sniffer.expectMsgFrom(replicas[1], ReplicaElectionAck.class, replicas[2]);

                    // 2 -> 3
                    sniffer.expectMsgFrom(replicas[0], ReplicaElection.class, replicas[2]);
                    sniffer.expectMsgFrom(replicas[2], ReplicaElectionAck.class, replicas[0]);

                    // Sync
                    sniffer.expectMsgFrom(replicas[1], MasterSync.class, replicas[0]);
                    sniffer.expectMsgFrom(replicas[2], MasterSync.class, replicas[0]);

                    return null;
                });
            }
        };
    }

}
