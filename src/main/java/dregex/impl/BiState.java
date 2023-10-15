package dregex.impl;

import java.util.Objects;

public final class BiState implements State {

    public final State first;
    public final State second;

    public BiState(State first, State second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return String.format("%s,%s", first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiState biState = (BiState) o;
        return Objects.equals(first, biState.first) && Objects.equals(second, biState.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}