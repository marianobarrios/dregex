package dregex.impl.tree;

import dregex.impl.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class Lit extends AbstractRange {

    public final int codePoint;

    public Lit(int codePoint) {
        this.codePoint = codePoint;
    }

    @Override
    public int from() {
        return codePoint;
    }

    @Override
    public int to() {
        return codePoint;
    }

    @Override
    public String toCharClassLit() {
        return toRegex();
    }

    @Override
    public int precedence() {
        return 1;
    }

    public Node canonical() {
        return this;
    }

    @Override
    public Node caseNormalize(Normalizer normalizer) {
        var str = Character.toString(codePoint);
        var normStr = normalizer.normalize(str);
        if (str.contentEquals(normStr)) {
            return this;
        } else {
            var normCodePoints = normStr.codePoints().toArray();
            if (normCodePoints.length > 1) {
                return new Juxt(Arrays.stream(normCodePoints).mapToObj(Lit::new).collect(Collectors.toList()));
            } else {
                return new Lit(normCodePoints[0]);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Lit(%d)", codePoint);
    }

    @Override
    public String toRegex() {
        if (Character.isLetterOrDigit(codePoint)) return new String(Character.toChars(codePoint));
        else return String.format("\\x{%X}", codePoint);
    }

    public static Lit fromSingletonString(CharSequence str) {
        if (Character.codePointCount(str, 0, str.length()) > 1) {
            throw new IllegalArgumentException("String is no char: " + str);
        }
        return new Lit(Character.codePointAt(str, 0));
    }
}
