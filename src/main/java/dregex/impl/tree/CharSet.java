package dregex.impl.tree;

import dregex.impl.Normalizer;
import dregex.impl.RangeOps;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CharSet implements Node {

    public final List<AbstractRange> ranges;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharSet charSet = (CharSet) o;
        return Objects.equals(ranges, charSet.ranges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ranges);
    }

    public CharSet(List<AbstractRange> ranges) {
        this.ranges = ranges;
    }

    public CharSet(AbstractRange... ranges) {
        this.ranges = Arrays.asList(ranges);
    }

    public CharSet complement() {
        return new CharSet(RangeOps.diff(Wildcard.instance, this.ranges));
    }

    @Override
    public String toRegex() {
        return String.format(
                "[%s]", ranges.stream().map(r -> r.toCharClassLit()).collect(Collectors.joining()));
    }

    @Override
    public CharSet canonical() {
        return this;
    }

    @Override
    public int precedence() {
        return 1;
    }

    @Override
    public Node caseNormalize(Normalizer normalizer) {
        return new Disj(ranges.stream().map(r -> r.caseNormalize(normalizer)).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return String.format(
                "%s(%s)",
                getClass().getSimpleName(),
                ranges.stream().map(r -> r.toString()).collect(Collectors.joining(", ")));
    }

    public static CharSet fromCharSets(CharSet... charSets) {
        return new CharSet(
                Arrays.stream(charSets).flatMap(x -> x.ranges.stream()).collect(Collectors.toList()));
    }
}
