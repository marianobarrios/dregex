package dregex.impl.tree;

import dregex.impl.CaseNormalization;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Juxt implements Node {

    public final List<? extends Node> values;

    public Juxt(List<? extends Node> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format("Juxt(%s)", values.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    @Override
    public Node canonical() {
        if (values.size() == 1) {
            return values.get(0).canonical();
        } else {
            return new Juxt(flattenValues(values).map(v -> v.canonical()).collect(Collectors.toList()));
        }
    }

    private Stream<Node> flattenValues(List<? extends Node> values) {
        return values.stream().flatMap(value -> {
            if (value instanceof Juxt) {
                return flattenValues(((Juxt) value).values);
            } else {
                return Stream.of(value);
            }
        });
    }

    @Override
    public String toRegex() {
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

    @Override
    public int precedence() {
        return 3;
    }

    @Override
    public Node caseNormalize(CaseNormalization normalizer) {
        return new Juxt(values.stream().map(v -> v.caseNormalize(normalizer)).collect(Collectors.toList()));
    }

    @Override
    public Node unicodeNormalize() {
        return new Juxt(values.stream().map(v -> v.unicodeNormalize()).collect(Collectors.toList()));
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

    public static Node of(Node... values) {
        return of(Arrays.stream(values).collect(Collectors.toList()));
    }

    public static Node of(List<? extends Node> values) {
        if (values.size() == 1) {
            return values.get(0);
        } else {
            return new Juxt(values);
        }
    }
}
