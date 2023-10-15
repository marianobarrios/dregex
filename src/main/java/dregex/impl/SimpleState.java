package dregex.impl;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class SimpleState implements State {

    public final int id = counter.getAndIncrement();

    @Override
    public String toString() {
        return toSubscriptString(id);
    }

    private String toSubscriptString(int number) {
        var string = Integer.toString(number);
        var ret = new StringBuffer();
        string.codePoints().forEach(codePoint -> {
            ret.append(Character.toChars(codePoint + 8272));
        });
        return ret.toString();
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
