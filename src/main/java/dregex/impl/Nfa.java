package dregex.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class Nfa {

    public static class Transition {

        public final State from;
        public final State to;
        public final AtomPart ch;

        public Transition(State from, State to, AtomPart ch) {
            this.from = from;
            this.to = to;
            this.ch = ch;
        }
    }

    public final State initial;

    public final Collection<Transition> transitions;

    public final Set<State> accepting;

    public Nfa(State initial, Collection<Transition> transitions, Set<State> accepting) {
        this.initial = initial;
        this.transitions = transitions;
        this.accepting = accepting;
    }

    @Override
    public String toString() {
        var transStr = transitions.stream().map(x -> x.toString()).collect(Collectors.joining("[", "; ", "]"));
        var acceptStr = accepting.stream().map(x -> x.toString()).collect(Collectors.joining("{", "; ", "}"));
        return String.format("initial: %s; transitions: %s; accepting: %s", initial, transStr, acceptStr);
    }

    public Set<State> collectAllStates() {
        Set<State> ret = new HashSet<>();
        ret.add(initial);
        ret.addAll(transitions.stream().map(x -> x.from).collect(Collectors.toList()));
        ret.addAll(transitions.stream().map(x -> x.to).collect(Collectors.toList()));
        ret.addAll(accepting);
        return ret;
    }
}
