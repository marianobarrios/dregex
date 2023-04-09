package dregex.impl.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Juxt implements Node {

    public final List<Node> values;

    public <A extends Node> Juxt(List<A> values) {
        this.values = new ArrayList<>(values);
    }

    @Override public String toString() {
        return String.format("Juxt(%s)", values.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    @Override public Node canonical() {
      return new Juxt(flattenValues(values).map(v -> v.canonical()).collect(Collectors.toList()));
    }

    private Stream<Node> flattenValues(List<Node> values) {
        return values.stream().flatMap(value -> {
            if (value instanceof Juxt) {
                return flattenValues(((Juxt) value).values);
            } else {
                return Stream.of(value);
            }
        });
    }

    @Override public String toRegex() {
        StringBuilder ret = new StringBuilder();
        for (var value : values) {
            if (value.precedence() > this.precedence()) {
                ret.append(String.format("(?:%s)", value.toRegex()));
            } else {
                ret.append(value.toRegex());
            }
        }
        return ret.toString();
    }

    @Override public int precedence() {
        return 3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Juxt juxt = (Juxt) o;
        return Objects.equals(values, juxt.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
