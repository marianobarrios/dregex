package dregex.impl;


import dregex.InvalidRegexException;
import dregex.impl.tree.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Take a regex AST and produce a NFA.
 * Except when noted the Thompson-McNaughton-Yamada algorithm is used.
 * <a href="http://stackoverflow.com/questions/11819185/steps-to-creating-an-nfa-from-a-regular-expression">Reference</a>
 */
public class Compiler {

    private final Map<AbstractRange, List<CharInterval>> intervalMapping;

    public Compiler(Map<AbstractRange, List<CharInterval>> intervalMapping) {
        this.intervalMapping = intervalMapping;
    }

    /**
     * Transform a regular expression abstract syntax tree into a corresponding NFA
     */
    public Dfa fromTree(Node ast) {
        var initial = new SimpleState();
        var accepting = new SimpleState();
        var transitions = fromTreeImpl(ast, initial, accepting);
        var nfa = new Nfa(initial, transitions, Set.of(accepting));
        return DfaAlgorithms.rewriteWithSimpleStates(DfaAlgorithms.fromNfa(nfa));
    }

    public List<Nfa.Transition> fromTreeImpl(Node node, SimpleState from, SimpleState to) {
        if (node instanceof AbstractRange) {
            // base case
            var range = (AbstractRange) node;
            var intervals = intervalMapping.get(range);
            return intervals.stream().map(interval -> new Nfa.Transition(from, to, interval)).collect(Collectors.toList());
        } else if (node instanceof CharSet) {
            var set = (CharSet) node;
            return fromTreeImpl(new Disj(set.ranges), from, to);
        } else if (node instanceof Juxt) {
            // this optimization should be applied before the lookarounds are expanded to intersections and differences
            var juxt = (Juxt) node;
            return processJuxt(CompilerHelper.combineNegLookaheads(juxt), from, to);
        } else if (node instanceof Lookaround) {
            var la = (Lookaround) node;
            return fromTreeImpl(new Juxt(la), from, to);
        } else if (node instanceof Disj) {
            var disj = (Disj) node;
            return processDisj(disj, from, to);
        } else if (node instanceof Rep) {
            var rep = (Rep) node;
            return processRep(rep, from, to);
        } else if (node instanceof Intersection) {
            var intersection = (Intersection) node;
            return processOp(DfaAlgorithms::doIntersect, intersection.left, intersection.right, from, to);
        } else if (node instanceof Union) {
            var union = (Union) node;
            return processOp(DfaAlgorithms::union, union.left, union.right, from, to);
        } else if (node instanceof Difference) {
            var difference = (Difference) node;
            return processOp(DfaAlgorithms::diff, difference.left, difference.right, from, to);
        } else if (node instanceof PositionalCaptureGroup) {
            var cg = (PositionalCaptureGroup) node;
            return processCaptureGroup(cg.value, from, to);
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
    List<Nfa.Transition> processJuxt(Juxt juxt, SimpleState from, SimpleState to) {
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
                        return fromTreeImpl(rightSide, from, to);
                    } else {
                        List<Node> nodes = new ArrayList<>(prefix.size() + 1);
                        nodes.addAll(prefix);
                        nodes.add(rightSide);
                        return fromTreeImpl(new Juxt(nodes), from, to);
                    }
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
                        return fromTreeImpl(leftSide, from, to);
                    } else {
                        List<Node> nodes = new ArrayList<>(suffix.size() + 1);
                        nodes.add(leftSide);
                        nodes.addAll(suffix);
                        return fromTreeImpl(new Juxt(nodes), from, to);
                    }
                default:
                    throw new IllegalStateException();
            }
        } else {
            return processJuxtNoLookaround(juxt, from, to);
        }
    }

    public List<Nfa.Transition> processJuxtNoLookaround(Juxt juxt, SimpleState from, SimpleState to) {
        if (juxt.values.isEmpty()) {
            return List.of(new Nfa.Transition(from, to, Epsilon.instance));
        } else if (juxt.values.size() == 1) {
            return fromTreeImpl(juxt.values.get(0), from, to);
        } else {
            // doing this iteratively prevents stack overflows in the case of long literal strings
            List<Nfa.Transition> transitions = new ArrayList<>();
            var prev = from;
            for (var part : juxt.values.subList(0, juxt.values.size() - 1)) {
                var intermediate = new SimpleState();
                transitions.addAll(fromTreeImpl(part, prev, intermediate));
                prev = intermediate;
            }
            transitions.addAll(fromTreeImpl(juxt.values.get(juxt.values.size() - 1), prev, to));
            return transitions;
        }
    }

    private List<Nfa.Transition> processOp(BiFunction<Dfa, Dfa, Dfa> operation, Node left, Node right, SimpleState from, SimpleState to) {
        var leftDfa = fromTree(left);
        var rightDfa = fromTree(right);
        var result = DfaAlgorithms.toNfa(operation.apply(leftDfa, rightDfa));
        List<Nfa.Transition> ret = new ArrayList<>();
        ret.addAll(result.transitions);
        ret.addAll(result.accepting.stream().map(acc -> new Nfa.Transition(acc, to, Epsilon.instance)).collect(Collectors.toList()));
        ret.add(new Nfa.Transition(from, result.initial, Epsilon.instance));
        return ret;
    }

    private List<Nfa.Transition> processCaptureGroup(Node value, SimpleState from, SimpleState to) {
        var int1 = new SimpleState();
        var int2 = new SimpleState();
        List<Nfa.Transition> ret = new ArrayList<>();
        ret.addAll(fromTreeImpl(value, int1, int2));
        ret.add(new Nfa.Transition(from, int1, Epsilon.instance));
        ret.add(new Nfa.Transition(int2, to, Epsilon.instance));
        return ret;
    }


    private List<Nfa.Transition> processRep(Rep rep, SimpleState from, SimpleState to) {
        if (rep.min == 1 && rep.max.isPresent() && rep.max.get() == 1) {

            return fromTreeImpl(rep.value, from, to);

        } else if (rep.min == 0 && rep.max.isPresent() && rep.max.get() == 0) {

            return List.of(new Nfa.Transition(from, to, Epsilon.instance));

        } else if (rep.min > 1 && rep.max.isEmpty()) {

            List<Node> juxtValues = new ArrayList<>(rep.min + 1);
            juxtValues.addAll(Collections.nCopies(rep.min, rep.value));
            juxtValues.add(new Rep(0, Optional.empty(), rep.value));
            return fromTreeImpl(new Juxt(juxtValues), from, to);

        } else if (rep.min == 1 && rep.max.isEmpty()) {

            var int1 = new SimpleState();
            var int2 = new SimpleState();
            List<Nfa.Transition> ret = new ArrayList<>();
            ret.addAll(fromTreeImpl(rep.value, int1, int2));
            ret.add(new Nfa.Transition(from, int1, Epsilon.instance));
            ret.add(new Nfa.Transition(int2, to, Epsilon.instance));
            ret.add(new Nfa.Transition(int2, int1, Epsilon.instance));
            return ret;

        } else if (rep.min == 0 && rep.max.isEmpty()) {

            var int1 = new SimpleState();
            var int2 = new SimpleState();
            List<Nfa.Transition> ret = new ArrayList<>();
            ret.addAll(fromTreeImpl(rep.value, int1, int2));
            ret.add(new Nfa.Transition(from, int1, Epsilon.instance));
            ret.add(new Nfa.Transition(int2, to, Epsilon.instance));
            ret.add(new Nfa.Transition(from, to, Epsilon.instance));
            ret.add(new Nfa.Transition(int2, int1, Epsilon.instance));
            return ret;

        } else if (rep.min > 1 && rep.max.isPresent()) {

            int x = rep.min - 1;
            List<Node> juxtValues = new ArrayList<>(x + 1);
            juxtValues.addAll(Collections.nCopies(x, rep.value));
            juxtValues.add(new Rep(1, Optional.of(rep.max.get() - x), rep.value));
            return fromTreeImpl(new Juxt(juxtValues), from, to);

        } else if (rep.min == 1 && rep.max.isPresent() && rep.max.get() > 0) {

            // doing this iteratively prevents stack overflows in the case of long repetitions
            var int1 = new SimpleState();
            List<Nfa.Transition> ret = new ArrayList<>();
            ret.addAll(fromTreeImpl(rep.value, from, int1));
            var prev = int1;
            for (int i = 1; i < rep.max.get() - 1; i++) {
                var intermediate = new SimpleState();
                ret.addAll(fromTreeImpl(rep.value, prev, intermediate));
                ret.add(new Nfa.Transition(prev, to, Epsilon.instance));
                prev = intermediate;
            }
            ret.addAll(fromTreeImpl(rep.value, prev, to));
            ret.add(new Nfa.Transition(prev, to, Epsilon.instance));
            return ret;

        } else if (rep.min == 0 && rep.max.isPresent() && rep.max.get() > 0) {

            // doing this iteratively prevents stack overflows in the case of long repetitions
            List<Nfa.Transition> ret = new ArrayList<>();
            var prev = from;
            for (int i = 0; i < rep.max.get() - 1; i++) {
                var intermediate = new SimpleState();
                ret.addAll(fromTreeImpl(rep.value, prev, intermediate));
                ret.add(new Nfa.Transition(prev, to, Epsilon.instance));
                prev = intermediate;
            }
            ret.addAll(fromTreeImpl(rep.value, prev, to));
            ret.add(new Nfa.Transition(prev, to, Epsilon.instance));
            return ret;

        } else {
            throw new IllegalArgumentException();
        }

    }
    
    private List<Nfa.Transition> processDisj(Disj disj, SimpleState from, SimpleState to) {
        return disj.values.stream().flatMap(part -> fromTreeImpl(part, from, to).stream()).collect(Collectors.toList());
    }

}
