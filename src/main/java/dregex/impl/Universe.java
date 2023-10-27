package dregex.impl;

import dregex.impl.tree.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The purpose of this class is to enforce that set operation between regular expressions are only done when it is
 * legal to do so, that is, when the regex are compatible.
 * <p>
 * The way this is enforced is that every compiled regular expression contains a reference to a [[Universe]], and
 * only expressions with the same universe are allowed to mix in set operation.
 * <p>
 * The same {@link Universe} ensures the same "alphabet" and [[Normalizer]] rules. Regular expressions compiled as a
 * group will always have the same universe.
 * <p>
 * In general, dealing with this class or calling the constructor is not necessary; a call to one of the `compile`
 * methods is simpler and more direct. However, there are cases in which the intermediate [[ParsedRegex]]s are
 * needed. Most notably, when caching [[CompiledRegex]] instances (which are in general more expensive to create).
 */
public class Universe {

    public static final Universe Empty = new Universe(List.of(), Normalization.NoNormalization);

    private final List<Node> parsedTrees;

    private final Normalizer normalization;

    private final Map<AbstractRange, List<CharInterval>> alphabet;

    public Universe(List<Node> parsedTrees, Normalizer normalization) {
        this.parsedTrees = List.copyOf(parsedTrees);
        this.normalization = normalization;
        var ranges = parsedTrees.stream().flatMap(t -> collect(t)).collect(Collectors.toList());
        this.alphabet = Map.copyOf(CharInterval.calculateNonOverlapping(ranges));
    }

    public List<Node> getParsedTrees() {
        return parsedTrees;
    }

    public Normalizer getNormalization() {
        return normalization;
    }

    public Map<AbstractRange, List<CharInterval>> getAlphabet() {
        return alphabet;
    }

    /**
     * Regular expressions can have character classes and wildcards. In order to produce a NFA, they should be expanded
     * to disjunctions. As the base alphabet is Unicode, just adding a wildcard implies a disjunction of more than one
     * million code points. Same happens with negated character classes or normal classes with large ranges.
     * <p>
     * To prevent this, the sets are not expanded to all characters individually, but only to disjoint intervals.
     * <p>
     * Example:
     * <p>
     * [abc]     -> a-c
     * [^efg]    -> 0-c|h-MAX
     * mno[^efg] -> def(0-c|h-l|m|n|o|p-MAX)
     * .         -> 0-MAX
     * <p>
     * Care must be taken when the regex is meant to be used for an operation with another regex (such as intersection
     * or difference). In this case, the sets must be disjoint across all the "universe"
     * <p>
     * This method collects the interval, so they can then be made disjoint.
     */
    private Stream<AbstractRange> collect(Node ast) {
        // order important
        if (ast instanceof Lookaround) {
            var lookaround = (Lookaround) ast;
            return Stream.concat(collect(lookaround.value), Stream.of(Wildcard.instance));
        } else if (ast instanceof CaptureGroup) {
            var captureGroup = (CaptureGroup) ast;
            return collect(captureGroup.value);
        } else if (ast instanceof Operation) {
            var operation = (Operation) ast;
            return Stream.concat(collect(operation.left), collect(operation.right));
        } else if (ast instanceof Disj) {
            var disj = (Disj) ast;
            return disj.values.stream().flatMap(v -> collect(v));
        } else if (ast instanceof Juxt) {
            var juxt = (Juxt) ast;
            return juxt.values.stream().flatMap(v -> collect(v));
        } else if (ast instanceof Rep) {
            var rep = (Rep) ast;
            return collect(rep.value);
        } else if (ast instanceof AbstractRange) {
            var range = (AbstractRange) ast;
            return Stream.of(range);
        } else if (ast instanceof CharSet) {
            var set = (CharSet) ast;
            return set.ranges.stream();
        } else {
            throw new RuntimeException("Unexpected node type: " + ast.getClass().getName());
        }
    }
}
