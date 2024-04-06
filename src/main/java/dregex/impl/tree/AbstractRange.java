package dregex.impl.tree;

import dregex.impl.CaseNormalization;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A single char, non empty, i.e, excluding epsilon values
 */
public abstract class AbstractRange implements Node {

    public abstract int from();

    public abstract int to();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractRange)) {
            return false;
        }
        AbstractRange that = (AbstractRange) obj;
        return this.from() == that.from() && this.to() == that.to();
    }

    @Override
    public int hashCode() {
        return Objects.hash(from(), to());
    }

    public abstract String toCharClassLit();

    @Override
    public Node caseNormalize(CaseNormalization normalizer) {
        var ranges = caseNormalizeImpl(normalizer);
        if (ranges.size() == 1) {
            return ranges.get(0);
        } else {
            return new Disj(ranges);
        }
    }

    public List<AbstractRange> caseNormalizeImpl(CaseNormalization normalizer) {
        // optimization
        if (normalizer == CaseNormalization.NoNormalization) {
            return List.of(this);
        }

        List<Integer> codePoints = IntStream.rangeClosed(from(), to())
                .map(c -> normalizer.normalize(c))
                .boxed()
                .collect(Collectors.toSet())
                .stream()
                .sorted()
                .collect(Collectors.toList());

        List<AbstractRange> ranges = new ArrayList<>();
        int openRangeFrom = codePoints.get(0);
        int openRangeTo = openRangeFrom;
        for (int codePoint : codePoints.subList(1, codePoints.size())) {
            if (codePoint == openRangeTo + 1) {
                openRangeTo = codePoint;
            } else {
                ranges.add(AbstractRange.of(openRangeFrom, openRangeTo));
                openRangeFrom = codePoint;
                openRangeTo = codePoint;
            }
        }
        ranges.add(AbstractRange.of(openRangeFrom, openRangeTo));
        return ranges;
    }

    @Override
    public Node unicodeNormalize() {
        var alternatives = IntStream.rangeClosed(from(), to())
                .mapToObj(codePoint -> {
                    var normCodePoints = Normalizer.normalize(Character.toString(codePoint), Normalizer.Form.NFD)
                            .codePoints()
                            .mapToObj(Lit::new)
                            .collect(Collectors.toList());
                    if (normCodePoints.size() > 1) {
                        return new Juxt(normCodePoints);
                    } else {
                        return normCodePoints.get(0);
                    }
                })
                .collect(Collectors.toList());
        return new Disj(alternatives);
    }

    public static AbstractRange of(int from, int to) {
        if (from == to) {
            return new Lit(from);
        } else if (from == Character.MIN_CODE_POINT && to == Character.MAX_CODE_POINT) {
            return Wildcard.instance;
        } else {
            return new CharRange(from, to);
        }
    }
}
