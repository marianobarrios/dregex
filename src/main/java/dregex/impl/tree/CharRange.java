package dregex.impl.tree;

import dregex.impl.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class CharRange extends AbstractRange {

    public final int from;
    public final int to;

    public CharRange(int from, int to) {
        if (from > to) throw new IllegalArgumentException("'from' value cannot be larger than 'to'");
        this.from = from;
        this.to = to;
    }

    @Override
    public String toRegex() {
        throw new UnsupportedOperationException("Cannot express a range outside a char class");
    }

    @Override
    public int from() {
        return from;
    }

    @Override
    public int to() {
        return to;
    }

    @Override
    public String toCharClassLit() {
        return String.format("%s-%s", new Lit(from).toRegex(), new Lit(to).toRegex());
    }

    @Override
    public int precedence() {
        throw new UnsupportedOperationException("Cannot express a range outside a char class");
    }

    public Node canonical() {
        if (from() == to()) {
            return new Lit(from());
        } else if (from() == Character.MIN_CODE_POINT && to() == Character.MAX_CODE_POINT) {
            return Wildcard.instance;
        } else {
            return new CharSet(this);
        }
    }

    @Override
    public Node caseNormalize(Normalizer normalizer) {
        if (from == to) {
            var str = Character.toString(from);
            var normStr = normalizer.normalize(str);
            if (str.contentEquals(normStr)) {
                return this;
            } else {
                return getNode(normStr);
            }
        } else {
            for (int c = from; c <= to; c++) {
                var str = Character.toString(c);
                var normStr = normalizer.normalize(str);
                if (!str.contentEquals(normStr)) {
                    if (c > from) {
                        if (c < to) {
                            return new Disj(
                                    new CharRange(from, c - 1),
                                    getNode(normStr),
                                    new CharRange(c + 1, to).caseNormalize(normalizer));
                        } else {
                            return new Disj(new CharRange(from, c - 1), getNode(normStr));
                        }
                    } else {
                        return new Disj(getNode(normStr), new CharRange(c + 1, to).caseNormalize(normalizer));
                    }
                }
            }
            return this;
        }
    }

    private static Node getNode(CharSequence normStr) {
        var codePoints = normStr.codePoints().toArray();
        if (codePoints.length > 1) {
            return new Disj(Arrays.stream(codePoints).mapToObj(Lit::new).collect(Collectors.toList()));
        } else {
            return new Lit(codePoints[0]);
        }
    }

    @Override
    public String toString() {
        return String.format("%d-%d", from, to);
    }
}
