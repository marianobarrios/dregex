package dregex.impl;

import java.util.*;
import java.util.stream.Collectors;

public class Dfa<A extends State> {

    public final A initial;
    public final Map<A, TreeMap<CharInterval, A>> defTransitions;
    public final Set<A> accepting;

    public final boolean minimal;

    public Dfa(A initial, Map<A, TreeMap<CharInterval, A>> defTransitions, Set<A> accepting, boolean minimal) {
        this.initial = initial;
        this.defTransitions = defTransitions;
        this.accepting = accepting;
        this.minimal = minimal;
    }

    @Override
    public String toString() {
        return String.format("initial: %s; transitions: %s; accepting: %s", initial, defTransitions, accepting);
    }

    public Set<A> allStates() {
        var ret = new HashSet<A>();
        ret.add(initial);
        ret.addAll(defTransitions.keySet());
        ret.addAll(defTransitions.values().stream().flatMap(x -> x.values().stream()).collect(Collectors.toList()));
        ret.addAll(accepting);
        return ret;
    }

    public Set<A> allButAccepting() {
        var ret = new HashSet<>(allStates());
        ret.removeAll(accepting);
        return ret;
    }

    public Set<CharInterval> allChars() {
        return defTransitions.values().stream().flatMap(x -> x.keySet().stream()).collect(Collectors.toSet());
    }

    public int stateCount() {
        return allStates().size();
    }

    public Map<CharInterval, A> transitionMap(A state) {
        var ret = defTransitions.get(state);
        return ret == null ? Map.of() : ret;
    }


    /**
     * Match-nothing DFA
     */
    public static Dfa<SimpleState> nothingDfa = new Dfa<>(new SimpleState(), Map.of(), Set.of(), false);

}
