package dregex.impl;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleState implements State {

    public final int id = counter.getAndIncrement();

    @Override
    public String toString() {
        return Util.toSubscriptString(id);
    }

    private static final AtomicInteger counter = new AtomicInteger();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleState that = (SimpleState) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
