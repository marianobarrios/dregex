package dregex.impl.tree;

public class Wildcard extends AbstractRange {

    public static final Wildcard instance = new Wildcard();

    private Wildcard() {}

    @Override
    public int from() {
        return Character.MIN_CODE_POINT;
    }

    @Override
    public int to() {
        return Character.MAX_CODE_POINT;
    }

    @Override
    public String toRegex() {
        return ".";
    }

    @Override
    public String toCharClassLit() {
        throw new UnsupportedOperationException("Cannot express a wildcard inside a char class");
    }

    @Override
    public int precedence() {
        return 1;
    }

    @Override
    public String toString() {
        return "✶";
    }

}
