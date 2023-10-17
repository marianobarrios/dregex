package dregex.impl;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class MultiState implements State {

    public final Set<State> states;

    public MultiState(Set<State> states) {
        this.states = states;
    }

    @Override
    public String toString() {
        return states.stream().map(s -> s.toString()).collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiState that = (MultiState) o;
        return states.equals(that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hash(states);
    }
}
