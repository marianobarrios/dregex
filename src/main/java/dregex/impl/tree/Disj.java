package dregex.impl.tree;

import dregex.impl.CaseNormalization;
import java.util.ArrayList;
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

    private Disj(List<? extends Node> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format(
                "%s(%s)",
                getClass().getSimpleName(),
                values.stream().map(v -> v.toString()).collect(Collectors.joining(", ")));
    }

    public String toRegex() {
        return values.stream().map(v -> v.toRegex()).collect(Collectors.joining("|"));
    }

    @Override
    public Node canonical() {
        if (values.size() == 1) {
            return values.get(0).canonical();
        } else {
            var canonicalValues = flattenValues(values).map(v -> v.canonical()).collect(Collectors.toList());

            var sets = canonicalValues.stream()
                    .filter(v -> v instanceof CharRange || v instanceof Lit || v instanceof CharSet)
                    .flatMap(v -> {
                        // A disjunction of ranges, literals and sets can be expressed simpler as a compound char set
                        if (v instanceof AbstractRange) {
                            return Stream.of((AbstractRange) v);
                        } else if (v instanceof CharSet) {
                            return ((CharSet) v).ranges.stream();
                        } else {
                            throw new IllegalStateException();
                        }
                    })
                    .collect(Collectors.toList());

            var nonSets = canonicalValues.stream()
                    .filter(v -> !(v instanceof CharRange || v instanceof Lit || v instanceof CharSet))
                    .collect(Collectors.toList());

            if (sets.isEmpty()) {
                return Disj.of(nonSets);
            } else if (nonSets.isEmpty()) {
                return new CharSet(sets);
            } else {
                List<Node> values = new ArrayList<>(nonSets);
                values.add(new CharSet(sets));
                return Disj.of(values);
            }
        }
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

    @Override
    public Node caseNormalize(CaseNormalization normalizer) {
        return Disj.of(values.stream().map(v -> v.caseNormalize(normalizer)).collect(Collectors.toList()));
    }

    @Override
    public Node unicodeNormalize() {
        return Disj.of(values.stream().map(v -> v.unicodeNormalize()).collect(Collectors.toList()));
    }

    public static Node of(Node... values) {
        return of(Arrays.stream(values).collect(Collectors.toList()));
    }

    public static Node of(List<? extends Node> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (values.size() == 1) {
            return values.get(0);
        } else {
            return new Disj(values);
        }
    }
}
