package dregex.impl.tree;

import java.util.Objects;
import java.util.Optional;

public class Rep implements Node {

    public final int min;
    public final Optional<Integer> max;

    public final Node value;

    public Rep(int min, Optional<Integer> max, Node value) {
        if (min < 0) {
            throw new IllegalArgumentException();
        }
        if (max.isPresent() && min > max.get()) {
            throw new IllegalArgumentException();
        }
        this.min = min;
        this.max = max;
        this.value = value;
    }

    @Override
    public String toRegex() {
        String suffix;
        if (min == 0 && max.isEmpty()) {
            suffix = "*";
        } else if (min == 1 && max.isEmpty()) {
            suffix = "+";
        } else if (min > 1 && max.isEmpty()) {
            suffix = "{" + min + ",}";
        } else if (min == 0 && max.get() == 1) {
            suffix = "?";
        } else if (min == 1 && max.get() == 1) {
            suffix = "";
        } else if (min == max.get()) {
            suffix = "{" + min + "}";
        } else {
            suffix = "{" + min + "," + max.get() + "}";
        }

        /*
         * On top of precedence, check special case of nested repetitions,
         * that are actually a grammar singularity. E.g., "a++" (invalid)
         * vs. "(a+)+" (valid)
         */
        if (value.precedence() > this.precedence() || value instanceof Rep) {
            return String.format("(?:%s)%s", value.toRegex(), suffix);
        } else{
            return value.toRegex() + suffix;
        }
    }

    @Override
    public Node canonical() {
        if (min == 1 && max.isPresent() && max.get() == 1) {
            return value.canonical();
        } else {
            return new Rep(min, max, value.canonical());
        }
    }

    @Override public int precedence() {
        return 2;
    }

    @Override
    public String toString() {
        String range;
        if (max.isEmpty()) {
            range = String.format("%d-∞", min);
        } else if (max.get() == min) {
            range = Integer.toString(min);
        } else {
            range = String.format("%d-%d", min, max.get());
        }

        return String.format("%s(%s,%s)", getClass().getSimpleName(), range, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rep rep = (Rep) o;
        return min == rep.min && Objects.equals(max, rep.max) && Objects.equals(value, rep.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max, value);
    }
}
