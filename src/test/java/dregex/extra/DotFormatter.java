package dregex.extra;

import dregex.impl.Epsilon;
import dregex.impl.Nfa;

import java.util.stream.Collectors;

class DotFormatter {

    public static String format(Nfa nfa) {
        var states = nfa.collectAllStates().stream().map(state -> {
            String shape;
            if (state.equals(nfa.initial)) {
                shape = "square";
            } else {
                shape = "circle";
            }
            int peripheries;
            if (nfa.accepting.contains(state)) {
                peripheries = 2;
            } else {
                peripheries = 1;
            }
            return String.format("\"%s\"[shape = %s, peripheries = %s];", state, shape, peripheries);
        });
        var transitions = nfa.transitions.stream().map(transition -> {
            int weight;
            if (transition.ch == Epsilon.instance) {
                weight = 1;
            } else {
                weight = 2;
            }
            return String.format("\"%s\" -> \"%s\" [label=%s, weight=%s];", transition.from, transition.to, transition.ch, weight);
        });
        return String.format("digraph graphname { rankdir=LR; %s %s}",
                states.collect(Collectors.joining("\n")), transitions.collect(Collectors.joining("\n")));
    }

}
