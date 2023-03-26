package dregex.impl;

import java.util.Objects;

public class BiState<A extends State> implements State {

    public final A first;
    public final A second;

    public BiState(A first, A second) {
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
        BiState<?> biState = (BiState<?>) o;
        return Objects.equals(first, biState.first) && Objects.equals(second, biState.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}