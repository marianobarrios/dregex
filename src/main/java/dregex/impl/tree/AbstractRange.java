package dregex.impl.tree;

import java.util.Objects;

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

    public AbstractRange canonical() {
        if (this instanceof Lit) {
            return this;
        }
        if (this instanceof Wildcard) {
            return this;
        }
        if (from() == to()) {
            return new Lit(from());
        } else if (from() == Character.MIN_CODE_POINT && to() == Character.MAX_CODE_POINT) {
            return Wildcard.instance;
        } else {
            return this;
        }
    }

    public abstract String toCharClassLit();

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
