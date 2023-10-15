package dregex.impl.tree;

import java.util.List;
import java.util.Objects;

public class Lookaround implements Node {

    public final Direction dir;
    public final Condition cond;
    public final Node value;

    public Lookaround(Direction dir, Condition cond, Node value) {
        this.dir = dir;
        this.cond = cond;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lookaround that = (Lookaround) o;
        return dir == that.dir && cond == that.cond && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dir, cond, value);
    }

    @Override
    public String toRegex() {
        String dirStr;
        switch (dir) {
            case Ahead: dirStr = ""; break;
            case Behind: dirStr = "<"; break;
            default: throw new IllegalStateException();
        }
        String condStr;
        switch (cond) {
            case Negative: condStr = "!"; break;
            case Positive: condStr = "="; break;
            default: throw new IllegalStateException();
        }
        return String.format("(?%s%s%s)", dirStr, condStr, value.toRegex());
    }

    @Override
    public Lookaround canonical() {
        return new Lookaround(dir, cond, value.canonical());
    }

    @Override
    public int precedence() {
        return 1;
    }

}