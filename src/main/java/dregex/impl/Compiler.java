package dregex.impl;

import dregex.InvalidRegexException;
import dregex.impl.tree.*;
import java.time.Duration;
import java.util.*;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Take a regex AST and produce a NFA.
 * Except when noted the Thompson-McNaughton-Yamada algorithm is used.
 * <a href="http://stackoverflow.com/questions/11819185/steps-to-creating-an-nfa-from-a-regular-expression">Reference</a>
 */
public class Compiler {

    private static final Logger logger = LoggerFactory.getLogger(Compiler.class);

    private final Map<AbstractRange, List<CharInterval>> intervalMapping;

    public Compiler(Map<AbstractRange, List<CharInterval>> intervalMapping) {
        this.intervalMapping = Map.copyOf(intervalMapping);
    }

    /**
     * Transform a regular expression abstract syntax tree into a corresponding DFA
     */
    public Dfa fromTree(Node ast) {
        var start = System.nanoTime();
        var initial = new SimpleState();
        var accepting = new SimpleState();
        List<Nfa.Transition> transitions = new ArrayList<>();
        addTransitionsFromNode(transitions, ast, initial, accepting);
        var nfa = new Nfa(initial, transitions, Set.of(accepting));
        var dfa = DfaAlgorithms.rewriteWithSimpleStates(DfaAlgorithms.fromNfa(nfa));
        var time = Duration.ofNanos(System.nanoTime() - start);
        logger.trace("DFA compiled in {}", time);
        return dfa;
    }

    private void addTransitionsFromNode(List<Nfa.Transition> transitions, Node node, SimpleState from, SimpleState to) {
        if (node instanceof AbstractRange) {
            // base case
            var range = (AbstractRange) node;
            for (var interval : intervalMapping.get(range)) {
                transitions.add(new Nfa.Transition(from, to, interval));
            }
        } else if (node instanceof CharSet) {
            var set = (CharSet) node;
            addTransitionsFromNode(transitions, new Disj(set.ranges), from, to);
        } else if (node instanceof Juxt) {
            // this optimization should be applied before the lookarounds are expanded to intersections and differences
            var juxt = (Juxt) node;
            addTransitionsFromJuxt(transitions, CompilerHelper.combineNegLookaheads(juxt), from, to);
        } else if (node instanceof Lookaround) {
            var la = (Lookaround) node;
            addTransitionsFromNode(transitions, new Juxt(la), from, to);
        } else if (node instanceof Disj) {
            var disj = (Disj) node;
            addTransitionsFromDisj(transitions, disj, from, to);
        } else if (node instanceof Rep) {
            var rep = (Rep) node;
            addTransitionsFromRep(transitions, rep, from, to);
        } else if (node instanceof Intersection) {
            var intersection = (Intersection) node;
            addTransitionsFromOperation(
                    transitions, DfaAlgorithms::doIntersect, intersection.left, intersection.right, from, to);
        } else if (node instanceof Union) {
            var union = (Union) node;
            addTransitionsFromOperation(transitions, DfaAlgorithms::union, union.left, union.right, from, to);
        } else if (node instanceof Difference) {
            var difference = (Difference) node;
            addTransitionsFromOperation(transitions, DfaAlgorithms::diff, difference.left, difference.right, from, to);
        } else if (node instanceof PositionalCaptureGroup) {
            var cg = (PositionalCaptureGroup) node;
            addTransitionsFromCaptureGroup(transitions, cg.value, from, to);
        } else if (node instanceof NamedCaptureGroup) {
            throw new InvalidRegexException("named capture groups are not supported");
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Lookaround constructions are transformed in equivalent DFA operations, and the result of those trivially transformed
     * into a NFA again for insertion into the outer expression.
     * <p>
     * (?=B)C is transformed into C ∩ B.*
     * (?!B)C is transformed into C - B.*
     * A(?<=B) is transformed into A ∩ .*B
     * A(?<!B) is transformed into A - .*B
     * <p>
     * In the case of more than one lookaround, the transformation is applied recursively.
     * <p>
     * *
     * NOTE: Only lookahead is currently implemented
     */
    void addTransitionsFromJuxt(List<Nfa.Transition> transitions, Juxt juxt, SimpleState from, SimpleState to) {
        var lookaroundIdx = CompilerHelper.findLookaround(juxt.values);
        if (lookaroundIdx != -1) {
            var prefix = juxt.values.subList(0, lookaroundIdx);
            var suffix = juxt.values.subList(lookaroundIdx + 1, juxt.values.size());
            var wildcard = new Rep(0, Optional.empty(), Wildcard.instance);
            var lookaround = (Lookaround) juxt.values.get(lookaroundIdx);
            switch (lookaround.dir) {
                case Ahead:
                    Operation rightSide;
                    switch (lookaround.cond) {
                        case Positive:
                            rightSide = new Intersection(new Juxt(suffix), new Juxt(lookaround.value, wildcard));
                            break;
                        case Negative:
                            rightSide = new Difference(new Juxt(suffix), new Juxt(lookaround.value, wildcard));
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                    if (prefix.isEmpty()) {
                        addTransitionsFromNode(transitions, rightSide, from, to);
                    } else {
                        List<Node> nodes = new ArrayList<>(prefix.size() + 1);
                        nodes.addAll(prefix);
                        nodes.add(rightSide);
                        addTransitionsFromNode(transitions, new Juxt(nodes), from, to);
                    }
                    break;
                case Behind:
                    Operation leftSide;
                    switch (lookaround.cond) {
                        case Positive:
                            leftSide = new Intersection(new Juxt(prefix), new Juxt(lookaround.value, wildcard));
                            break;
                        case Negative:
                            leftSide = new Difference(new Juxt(prefix), new Juxt(lookaround.value, wildcard));
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                    if (suffix.isEmpty()) {
                        addTransitionsFromNode(transitions, leftSide, from, to);
                    } else {
                        List<Node> nodes = new ArrayList<>(suffix.size() + 1);
                        nodes.add(leftSide);
                        nodes.addAll(suffix);
                        addTransitionsFromNode(transitions, new Juxt(nodes), from, to);
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        } else {
            addTransitionsFromJuxtNoLookaround(transitions, juxt, from, to);
        }
    }

    public void addTransitionsFromJuxtNoLookaround(
            List<Nfa.Transition> transitions, Juxt juxt, SimpleState from, SimpleState to) {
        if (juxt.values.isEmpty()) {
            transitions.add(new Nfa.Transition(from, to, Epsilon.instance));
        } else if (juxt.values.size() == 1) {
            addTransitionsFromNode(transitions, juxt.values.get(0), from, to);
        } else {
            // doing this iteratively prevents stack overflows in the case of long literal strings
            var prev = from;
            for (var part : juxt.values.subList(0, juxt.values.size() - 1)) {
                var intermediate = new SimpleState();
                addTransitionsFromNode(transitions, part, prev, intermediate);
                prev = intermediate;
            }
            addTransitionsFromNode(transitions, juxt.values.get(juxt.values.size() - 1), prev, to);
        }
    }

    private void addTransitionsFromOperation(
            List<Nfa.Transition> transitions,
            BiFunction<Dfa, Dfa, Dfa> operation,
            Node left,
            Node right,
            SimpleState from,
            SimpleState to) {
        var leftDfa = fromTree(left);
        var rightDfa = fromTree(right);
        var result = DfaAlgorithms.toNfa(operation.apply(leftDfa, rightDfa));
        transitions.addAll(result.transitions);
        for (var acc : result.accepting) {
            transitions.add(new Nfa.Transition(acc, to, Epsilon.instance));
        }
        transitions.add(new Nfa.Transition(from, result.initial, Epsilon.instance));
    }

    private void addTransitionsFromCaptureGroup(
            List<Nfa.Transition> transitions, Node value, SimpleState from, SimpleState to) {
        var int1 = new SimpleState();
        var int2 = new SimpleState();
        transitions.add(new Nfa.Transition(from, int1, Epsilon.instance));
        transitions.add(new Nfa.Transition(int2, to, Epsilon.instance));
        addTransitionsFromNode(transitions, value, int1, int2);
    }

    private void addTransitionsFromRep(List<Nfa.Transition> transitions, Rep rep, SimpleState from, SimpleState to) {
        if (rep.min == 1 && rep.max.isPresent() && rep.max.get() == 1) {

            addTransitionsFromNode(transitions, rep.value, from, to);

        } else if (rep.min == 0 && rep.max.isPresent() && rep.max.get() == 0) {

            transitions.add(new Nfa.Transition(from, to, Epsilon.instance));

        } else if (rep.min > 1 && rep.max.isEmpty()) {

            List<Node> juxtValues = new ArrayList<>(rep.min + 1);
            juxtValues.addAll(Collections.nCopies(rep.min, rep.value));
            juxtValues.add(new Rep(0, Optional.empty(), rep.value));
            addTransitionsFromNode(transitions, new Juxt(juxtValues), from, to);

        } else if (rep.min == 1 && rep.max.isEmpty()) {

            var int1 = new SimpleState();
            var int2 = new SimpleState();
            transitions.add(new Nfa.Transition(from, int1, Epsilon.instance));
            transitions.add(new Nfa.Transition(int2, to, Epsilon.instance));
            transitions.add(new Nfa.Transition(int2, int1, Epsilon.instance));
            addTransitionsFromNode(transitions, rep.value, int1, int2);

        } else if (rep.min == 0 && rep.max.isEmpty()) {

            var int1 = new SimpleState();
            var int2 = new SimpleState();
            transitions.add(new Nfa.Transition(from, int1, Epsilon.instance));
            transitions.add(new Nfa.Transition(int2, to, Epsilon.instance));
            transitions.add(new Nfa.Transition(from, to, Epsilon.instance));
            transitions.add(new Nfa.Transition(int2, int1, Epsilon.instance));
            addTransitionsFromNode(transitions, rep.value, int1, int2);

        } else if (rep.min > 1 && rep.max.isPresent()) {

            int x = rep.min - 1;
            List<Node> juxtValues = new ArrayList<>(x + 1);
            juxtValues.addAll(Collections.nCopies(x, rep.value));
            juxtValues.add(new Rep(1, Optional.of(rep.max.get() - x), rep.value));
            addTransitionsFromNode(transitions, new Juxt(juxtValues), from, to);

        } else if (rep.min == 1 && rep.max.isPresent() && rep.max.get() > 0) {

            // doing this iteratively prevents stack overflows in the case of long repetitions
            var int1 = new SimpleState();
            addTransitionsFromNode(transitions, rep.value, from, int1);
            var prev = int1;
            for (int i = 1; i < rep.max.get() - 1; i++) {
                var intermediate = new SimpleState();
                transitions.add(new Nfa.Transition(prev, to, Epsilon.instance));
                addTransitionsFromNode(transitions, rep.value, prev, intermediate);
                prev = intermediate;
            }
            transitions.add(new Nfa.Transition(prev, to, Epsilon.instance));
            addTransitionsFromNode(transitions, rep.value, prev, to);

        } else if (rep.min == 0 && rep.max.isPresent() && rep.max.get() > 0) {

            // doing this iteratively prevents stack overflows in the case of long repetitions
            var prev = from;
            for (int i = 0; i < rep.max.get() - 1; i++) {
                var intermediate = new SimpleState();
                transitions.add(new Nfa.Transition(prev, to, Epsilon.instance));
                addTransitionsFromNode(transitions, rep.value, prev, intermediate);
                prev = intermediate;
            }
            transitions.add(new Nfa.Transition(prev, to, Epsilon.instance));
            addTransitionsFromNode(transitions, rep.value, prev, to);

        } else {
            throw new IllegalArgumentException();
        }
    }

    private void addTransitionsFromDisj(List<Nfa.Transition> transitions, Disj disj, SimpleState from, SimpleState to) {
        for (Node part : disj.values) {
            addTransitionsFromNode(transitions, part, from, to);
        }
    }
}
