package dregex.impl.tree;

import java.util.Optional;

public class Quantification {

    public final int min;
    public final Optional<Integer> max;

    public Quantification(int min) {
        this.min = min;
        this.max = Optional.empty();
    }

    public Quantification(int min, int max) {
        this.min = min;
        this.max = Optional.of(max);
    }
}
