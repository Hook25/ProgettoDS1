package it.unitn.ds1.project;

import java.util.Comparator;
import java.util.Objects;

import static java.util.Comparator.comparingInt;

public class Timestamp implements Comparable<Timestamp> {

    /**
     * referred as 'i' in the project document
     */
    public final int counter;

    /**
     * referred as 'e' in the project document
     */
    public final int epoch;

    public Timestamp(int counter, int epoch) {
        this.counter = counter;
        this.epoch = epoch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Timestamp timestamp = (Timestamp) o;
        return counter == timestamp.counter &&
                epoch == timestamp.epoch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(counter, epoch);
    }

    @Override
    public String toString() {
        return String.format("%d:%d", epoch, counter);
    }

    private static final Comparator<Timestamp> COMPARATOR =
            comparingInt((Timestamp timestamp) -> timestamp.epoch)
                    .thenComparingInt(timestamp -> timestamp.counter);

    @Override
    public int compareTo(Timestamp o) {
        return COMPARATOR.compare(this, o);
    }

    public Timestamp nextUpdate() {
        return new Timestamp(counter + 1, epoch);
    }
}
