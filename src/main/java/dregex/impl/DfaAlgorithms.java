package dregex.impl;

import dregex.MatchResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DfaAlgorithms {

    public static Dfa doIntersect(Dfa left, Dfa right) {
        return removeUnreachableStates(doIntersection(left, right));
    }

    public static Dfa union(Dfa left, Dfa right) {
        return removeUnreachableStates(doUnion(left, right));
    }

    public static Dfa diff(Dfa left, Dfa right) {
        return removeUnreachableStates(doDifference(left, right));
    }

    /**
     * Intersections, unions and differences between DFA are done using the "product construction"
     * The following pages include graphical examples of this technique:
     * <a href="https://stackoverflow.com/q/7780521/4505326">...</a>
     * <a href="https://cs.stackexchange.com/a/7108">...</a>
     */
    private static Dfa productConstruction(Dfa left, Dfa right, BiPredicate<State, State> acceptingStateFilter) {
        Set<CharInterval> allChars = setUnion(left.allChars(), right.allChars());

        BiState newInitial = new BiState(left.initial, right.initial);

        Collection<State> allLeftStates = getAllStatesWithNullState(left);
        Collection<State> allRightStates = getAllStatesWithNullState(right);

        Map<State, TreeMap<CharInterval, State>> newTransitions =
                new HashMap<>(allLeftStates.size() * allRightStates.size());
        for (var leftState : allLeftStates) {
            Map<CharInterval, State> leftCharMap = left.transitionMap(leftState);

            for (var rightState : allRightStates) {
                Map<CharInterval, State> rightCharMap = right.transitionMap(rightState);

                TreeMap<CharInterval, State> charMap = new TreeMap<>();
                for (var ch : allChars) {
                    State leftDestState = leftCharMap.get(ch);
                    State rightDestState = rightCharMap.get(ch);
                    charMap.put(ch, new BiState(leftDestState, rightDestState));
                }

                newTransitions.put(new BiState(leftState, rightState), charMap);
            }
        }

        Set<State> newAccepting = new HashSet<>();
        for (var leftState : allLeftStates) {
            for (var rightState : allRightStates) {
                if (acceptingStateFilter.test(leftState, rightState)) {
                    newAccepting.add(new BiState(leftState, rightState));
                }
            }
        }

        return new Dfa(newInitial, newTransitions, newAccepting, false);
    }

    private static Dfa doIntersection(Dfa left, Dfa right) {
        return productConstruction(left, right, (l, r) -> left.accepting.contains(l) && right.accepting.contains(r));
    }

    public static Dfa doDifference(Dfa left, Dfa right) {
        return productConstruction(left, right, (l, r) -> left.accepting.contains(l) && !right.accepting.contains(r));
    }

    public static Dfa doUnion(Dfa left, Dfa right) {
        return productConstruction(left, right, (l, r) -> left.accepting.contains(l) || right.accepting.contains(r));
    }

    private static Collection<State> getAllStatesWithNullState(Dfa dfa) {
        var allStates = dfa.allStates();
        Collection<State> ret = new ArrayList<>(allStates.size() + 1);
        ret.addAll(allStates);
        ret.add(null);
        return ret;
    }

    public static Dfa removeUnreachableStates(Dfa dfa) {
        Set<State> visited = new HashSet<>();
        Queue<State> pending = new ArrayDeque<>();
        pending.add(dfa.initial);
        while (!pending.isEmpty()) {
            var currentState = pending.remove();
            visited.add(currentState);
            Collection<State> currentPossibleTargets =
                    new HashSet<>(dfa.transitionMap(currentState).values());
            for (var targetState : currentPossibleTargets) {
                if (!visited.contains(targetState)) {
                    pending.add(targetState);
                }
            }
        }
        var filteredTransitions = dfa.defTransitions.entrySet().stream()
                .filter(s -> visited.contains(s.getKey()))
                .collect(toMapCollector());
        var filteredAccepting =
                dfa.accepting.stream().filter(s -> visited.contains(s)).collect(Collectors.toSet());
        return new Dfa(dfa.initial, filteredTransitions, filteredAccepting, false);
    }

    public static boolean isIntersectionNotEmpty(Dfa left, Dfa right) {
        return matchesAtLeastOne(doIntersection(left, right));
    }

    /**
     * Return whether a DFA matches anything. A DFA matches at least some language if there is a path from the initial
     * state to any of the accepting states
     */
    public static boolean matchesAtLeastOne(Dfa dfa) {
        return hasPathToAccepting(new HashSet<>(), dfa, dfa.initial);
    }

    private static boolean hasPathToAccepting(Set<State> visited, Dfa dfa, State current) {
        if (dfa.accepting.contains(current)) {
            return true;
        } else {
            visited.add(current);
            for (var targetState : dfa.transitionMap(current).values()) {
                if (!visited.contains(targetState) && hasPathToAccepting(visited, dfa, targetState)) return true;
            }
            return false;
        }
    }

    public static boolean equivalent(Dfa left, Dfa right) {
        return !matchesAtLeastOne(doDifference(left, right)) && !matchesAtLeastOne(doDifference(right, left));
    }

    public static boolean isProperSubset(Dfa left, Dfa right) {
        return !matchesAtLeastOne(doDifference(left, right)) && matchesAtLeastOne(doDifference(right, left));
    }

    public static boolean isSubsetOf(Dfa left, Dfa right) {
        return !matchesAtLeastOne(doDifference(left, right));
    }

    public static <A> Set<A> setUnion(Set<A> left, Set<A> right) {
        Set<A> ret = new HashSet<>(left.size() + right.size()); // assumption
        ret.addAll(left);
        ret.addAll(right);
        return ret;
    }

    public static <K, U> Collector<Map.Entry<K, U>, ?, Map<K, U>> toMapCollector() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    public static Dfa rewriteWithSimpleStates(Dfa genericDfa) {
        return rewrite(genericDfa, () -> new SimpleState());
    }

    /**
     * Rewrite a DFA using canonical names for the states.
     * Useful for simplifying the DFA product of intersections or NFA conversions.
     * This function does not change the language matched by the DFA
     */
    public static Dfa rewrite(Dfa dfa, Supplier<State> stateFactory) {
        Map<State, State> mapping = dfa.allStates().stream().collect(Collectors.toMap(s -> s, s -> stateFactory.get()));
        Map<State, TreeMap<CharInterval, State>> newTransitions = new HashMap<>();
        for (var entry : dfa.defTransitions.entrySet()) {
            State s = entry.getKey();
            TreeMap<CharInterval, State> charMap = entry.getValue();
            var newCharMap = new TreeMap<>(charMap.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> mapping.get(e.getValue()))));
            newTransitions.put(mapping.get(s), newCharMap);
        }
        Set<State> newAccepting =
                dfa.accepting.stream().map(s -> mapping.get(s)).collect(Collectors.toSet());
        return new Dfa(mapping.get(dfa.initial), newTransitions, newAccepting, false);
    }

    public static MatchResult matchString(Dfa dfa, CharSequence string) {
        var current = dfa.initial;
        int i = 0;
        var it = string.codePoints().iterator();
        while (it.hasNext()) {
            int codePoint = it.next();
            TreeMap<CharInterval, State> currentTrans = dfa.defTransitions.get(current);
            State newState = null;
            if (currentTrans != null) {
                // O(log transitions) search in the range tree
                var entry = currentTrans.floorEntry(new CharInterval(codePoint, codePoint));
                if (entry != null) {
                    var interval = entry.getKey();
                    if (codePoint <= interval.to) {
                        newState = entry.getValue();
                    }
                }
            }
            if (newState == null) {
                return new MatchResult(false, i);
            }
            current = newState;
            i += 1;
        }
        return new MatchResult(dfa.accepting.contains(current), i);
    }

    public static MatchResult matchInputStream(Dfa dfa, InputStream inputStream) throws IOException {
        // Start from the initial state of the DFA
        State currentState = dfa.initial;

        // Current position in the input stream
        int position = 0;

        // Read the input stream character by character
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int ch;
            while ((ch = reader.read()) != -1) {
                // Read the next Unicode code point (handles surrogate pairs)
                int codePoint = readCodePoint(reader, ch);
                if (codePoint == -1) break; // End of stream or invalid surrogate pair

                // Get the next DFA state based on the current state and input character
                State nextState = getNextState(dfa, currentState, codePoint);

                if (nextState == null) {
                    // If not accepting, return failure at current position
                    return new MatchResult(false, position);
                }

                // Move to the next state and increment position
                currentState = nextState;
                position++;
            }
        }

        // After reading the stream, check if the current state is accepting
        return new MatchResult(dfa.accepting.contains(currentState), position);
    }

    // Reads a Unicode code point from the stream, handling surrogate pairs if needed
    private static int readCodePoint(InputStreamReader reader, int firstChar) throws IOException {
        char c1 = (char) firstChar;
        if (Character.isHighSurrogate(c1)) {
            int ch2 = reader.read();
            if (ch2 == -1) return -1; // Incomplete surrogate pair
            char c2 = (char) ch2;
            return Character.toCodePoint(c1, c2);
        }
        return c1;
    }

    // Retrieves the next DFA state based on the current state and input code point
    private static State getNextState(Dfa dfa, State current, int codePoint) {
        TreeMap<CharInterval, State> transitions = dfa.defTransitions.get(current);
        if (transitions == null) return null;

        // Find the transition whose interval includes the code point
        var entry = transitions.floorEntry(new CharInterval(codePoint, codePoint));
        if (entry != null && codePoint <= entry.getKey().to) {
            return entry.getValue();
        }
        return null;
    }

    /**
     * Each DFA is also trivially a NFA, return it.
     */
    public static Nfa toNfa(Dfa dfa) {
        Collection<Nfa.Transition> nfaTransitions = new ArrayList<>();
        for (var entry1 : dfa.defTransitions.entrySet()) {
            var state = entry1.getKey();
            var transitionMap = entry1.getValue();
            for (var entry2 : transitionMap.entrySet()) {
                var ch = entry2.getKey();
                var target = entry2.getValue();
                nfaTransitions.add(new Nfa.Transition(state, target, ch));
            }
        }
        return new Nfa(dfa.initial, nfaTransitions, new HashSet<>(dfa.accepting));
    }

    public static Dfa reverseAsDfa(Dfa dfa) {
        return DfaAlgorithms.fromNfa(DfaAlgorithms.reverse(dfa));
    }

    /**
     * DFA minimization, using
     * <a href="http://cs.stackexchange.com/questions/1872/brzozowskis-algorithm-for-dfa-minimization">Brzozowski's algorithm</a>
     */
    public static Dfa minimize(Dfa dfa) {
        if (dfa.minimal) {
            return dfa;
        } else {
            var reversedDfa = reverseAsDfa(dfa);
            var doubleReversedDfa = reverseAsDfa(reversedDfa);
            var minimalDfa = new Dfa(
                    doubleReversedDfa.initial, doubleReversedDfa.defTransitions, doubleReversedDfa.accepting, true);
            return rewriteWithSimpleStates(minimalDfa);
        }
    }

    public static Nfa reverse(Dfa dfa) {
        var initial = new SimpleState();
        Set<Nfa.Transition> nfaTransitions = dfa.accepting.stream()
                .map(s -> new Nfa.Transition(initial, s, Epsilon.instance))
                .collect(Collectors.toSet());
        for (var entry1 : dfa.defTransitions.entrySet()) {
            State from = entry1.getKey();
            var map = entry1.getValue();
            for (var entry2 : map.entrySet()) {
                CharInterval ch = entry2.getKey();
                State to = entry2.getValue();
                nfaTransitions.add(new Nfa.Transition(to, from, ch));
            }
        }
        return new Nfa(initial, nfaTransitions, Set.of(dfa.initial));
    }

    /**
     * Produce a DFA from a NFA using the
     * <a href="https://en.wikipedia.org/w/index.php?title=Powerset_construction&oldid=547783241">'power set construction'</a>
     */
    public static Dfa fromNfa(Nfa nfa) {
        /*
         * Group the list of transitions of the NFA into a nested map, for easy lookup.
         * The rest of this method will use this map instead of the original list.
         */
        Map<State, Map<AtomPart, Set<State>>> transitionMap = new HashMap<>();
        for (var entry : nfa.transitions.stream()
                .collect(Collectors.groupingBy(t -> t.from))
                .entrySet()) {
            State state = entry.getKey();
            var stateTransitions = entry.getValue();
            Map<AtomPart, Set<State>> map = new HashMap<>();
            for (var entry2 : stateTransitions.stream()
                    .collect(Collectors.groupingBy(x -> x.ch))
                    .entrySet()) {
                AtomPart atomPart = entry2.getKey();
                var atomTransitions = entry2.getValue();
                var states = atomTransitions.stream().map(t -> t.to).collect(Collectors.toSet());
                map.put(atomPart, states);
            }
            transitionMap.put(state, map);
        }

        Map<State, Map<CharInterval, Set<State>>> epsilonFreeTransitions = new HashMap<>();
        for (var entry : transitionMap.entrySet()) {
            var state = entry.getKey();
            var transitionMap2 = entry.getValue();
            Map<CharInterval, Set<State>> map = new HashMap<>();
            for (var entry2 : transitionMap2.entrySet()) {
                var ch = entry2.getKey();
                var target = entry2.getValue();
                if (ch instanceof CharInterval) {
                    map.put((CharInterval) ch, target);
                }
            }
            epsilonFreeTransitions.put(state, map);
        }

        Map<Set<State>, MultiState> epsilonExpansionCache = new HashMap<>();

        // Given a transition map and a set of states of a NFA, this function augments that set, following all epsilon
        // transitions recursively
        Function<Set<State>, MultiState> followEpsilon = current -> {
            return epsilonExpansionCache.computeIfAbsent(current, c -> followEpsilonImpl(transitionMap, c));
        };

        var dfaInitial = followEpsilon.apply(Set.of(nfa.initial));

        Map<State, TreeMap<CharInterval, State>> dfaTransitions = new HashMap<>();
        Set<MultiState> dfaStates = new HashSet<>();

        Queue<MultiState> pending = new ArrayDeque<>();
        pending.add(dfaInitial);
        while (!pending.isEmpty()) {
            MultiState current = pending.remove();
            dfaStates.add(current);
            // The set of all transition maps of the members of the current state
            Set<Map<CharInterval, Set<State>>> currentTrans = current.states.stream()
                    .map(x -> epsilonFreeTransitions.getOrDefault(x, Map.of()))
                    .collect(Collectors.toSet());

            // The transition function of the current state
            Map<CharInterval, Set<State>> mergedCurrentTrans = new HashMap<>();
            for (var transitions : currentTrans) {
                for (var entry : transitions.entrySet()) {
                    var charInterval = entry.getKey();
                    var states = entry.getValue();
                    mergedCurrentTrans
                            .computeIfAbsent(charInterval, x -> new HashSet<>())
                            .addAll(states);
                }
            }

            // use a temporary set before enqueueing to avoid adding the same state twice
            Set<MultiState> newPending = new HashSet<>();
            Map<CharInterval, MultiState> dfaCurrentTrans = new HashMap<>();
            for (var entry : mergedCurrentTrans.entrySet()) {
                var ch = entry.getKey();
                var states = entry.getValue();
                var targetState = followEpsilon.apply(states);
                if (!dfaStates.contains(targetState)) {
                    newPending.add(targetState);
                }
                dfaCurrentTrans.put(ch, targetState);
            }

            pending.addAll(newPending);

            if (!dfaCurrentTrans.isEmpty()) {
                dfaTransitions.put(current, new TreeMap<>(dfaCurrentTrans));
            }
        }
        // a DFA state is accepting if any of its NFA member-states is
        var dfaAccepting = dfaStates.stream()
                .filter(st -> Util.doIntersect(st.states, nfa.accepting))
                .collect(Collectors.toSet());

        return new Dfa(dfaInitial, dfaTransitions, dfaAccepting, false);
    }

    private static MultiState followEpsilonImpl(
            Map<State, Map<AtomPart, Set<State>>> transitionMap, Set<State> current) {
        Set<Set<State>> immediate = new HashSet<>();
        for (var state : current) {
            immediate.add(transitionMap.getOrDefault(state, Map.of()).getOrDefault(Epsilon.instance, Set.of()));
        }
        Set<State> expanded = immediate.stream().reduce(current, (a, b) -> Util.union(a, b));
        if (expanded.equals(current)) {
            return new MultiState(current);
        } else {
            return followEpsilonImpl(transitionMap, expanded);
        }
    }
}
