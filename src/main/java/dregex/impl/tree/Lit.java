package dregex.impl.tree;

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
