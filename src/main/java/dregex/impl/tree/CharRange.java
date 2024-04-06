package dregex.impl.tree;

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
    public String toString() {
        return String.format("%d-%d", from, to);
    }
}
