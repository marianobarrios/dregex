package dregex.impl;

import java.util.*;
import java.util.stream.Collectors;

public final class Dfa {

    public final State initial;
    public final Map<State, TreeMap<CharInterval, State>> defTransitions;
    public final Set<? extends State> accepting;

    public final boolean minimal;

    public Dfa(
            State initial,
            Map<State, TreeMap<CharInterval, State>> defTransitions,
            Set<? extends State> accepting,
            boolean minimal) {
        this.initial = initial;
        this.defTransitions = defTransitions;
        this.accepting = accepting;
        this.minimal = minimal;
    }

    @Override
    public String toString() {
        return String.format("initial: %s; transitions: %s; accepting: %s", initial, defTransitions, accepting);
    }

    public Set<State> allStates() {
        Set<State> ret = new HashSet<>();
        ret.add(initial);
        ret.addAll(defTransitions.keySet());
        ret.addAll(defTransitions.values().stream()
                .flatMap(x -> x.values().stream())
                .collect(Collectors.toList()));
        ret.addAll(accepting);
        return ret;
    }

    public Set<State> allButAccepting() {
        var ret = new HashSet<>(allStates());
        ret.removeAll(accepting);
        return ret;
    }

    public Set<CharInterval> allChars() {
        return defTransitions.values().stream()
                .flatMap(x -> x.keySet().stream())
                .collect(Collectors.toSet());
    }

    public int stateCount() {
        return allStates().size();
    }

    public Map<CharInterval, State> transitionMap(State state) {
        var ret = defTransitions.get(state);
        return ret == null ? Map.of() : ret;
    }

    /**
     * Match-nothing DFA
     */
    public static final Dfa nothingDfa = new Dfa(new SimpleState(), Map.of(), Set.of(), false);
}
