package dregex.impl.tree;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Disj implements Node {

    public final List<? extends Node> values;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Disj disj = (Disj) o;
        return Objects.equals(values, disj.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    public Disj(List<? extends Node> values) {
        this.values = values;
    }

    public Disj(Node... values) {
        this.values = Arrays.asList(values);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)",
                getClass().getSimpleName(),
                values.stream().map(v -> v.toString()).collect(Collectors.joining(", ")));
    }

    public String toRegex() {
        return values.stream().map(v -> v.toRegex()).collect(Collectors.joining("|"));
    }

    @Override
    public Node canonical() {
        return new Disj(flattenValues(values).map(v -> v.canonical()).collect(Collectors.toList()));
    }

    private Stream<Node> flattenValues(List<? extends Node> values) {
        return values.stream().flatMap(value -> {
            if (value instanceof Disj) {
                return flattenValues(((Disj) value).values);
            } else {
                return Stream.of(value);
            }
        });
    }

    @Override
    public int precedence() {
        return 4;
    }

}
