package it.unitn.ds1.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Update {
    public final Timestamp timestamp;
    public final int value;

    public Update(Timestamp timestamp, int value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public static List<Update> initialList() {
        return new ArrayList<>(Collections.singletonList(
                new Update(new Timestamp(0, 0), 0)
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Update update = (Update) o;
        return value == update.value &&
                timestamp.equals(update.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value);
    }

    @Override
    public String toString() {
        return "Update{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                '}';
    }
}
