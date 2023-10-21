package dregex.impl;

import dregex.impl.tree.AbstractRange;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RangeOps {

    public static List<AbstractRange> diff(List<AbstractRange> left, List<? extends AbstractRange> right) {
        return left.stream().flatMap(l -> diff(l, right).stream()).collect(Collectors.toList());
    }

    public static List<AbstractRange> diff(AbstractRange left, List<? extends AbstractRange> right) {
        return right.stream()
                .reduce(List.of(left), (a, b) -> diff(a, b), (a, b) -> Stream.concat(a.stream(), b.stream())
                        .collect(Collectors.toList()));
    }

    public static List<AbstractRange> diff(List<AbstractRange> left, AbstractRange right) {
        return left.stream().flatMap(l -> diff(l, right).stream()).collect(Collectors.toList());
    }

    public static List<AbstractRange> diff(AbstractRange left, AbstractRange right) {
        if (left.from() >= right.from() && left.to() <= right.to()) {
            return List.of();
        } else if (left.from() > right.to()) {
            return List.of(left);
        } else if (left.to() < right.from()) {
            return List.of(left);
        } else if (left.from() < right.from() && left.to() > right.to()) {
            return List.of(
                    AbstractRange.of(left.from(), right.from() - 1), AbstractRange.of(right.to() + 1, left.to()));
        } else if (left.from() < right.from() && left.to() <= right.to()) {
            return List.of(AbstractRange.of(left.from(), right.from() - 1));
        } else if (left.from() >= right.from() && left.to() > right.to()) {
            return List.of(AbstractRange.of(right.to() + 1, left.to()));
        } else {
            throw new AssertionError();
        }
    }

    /**
     * Minimize a sequence of (possibly sequential) ranges, returning a (hopefully) shorter sequence,
     * with overlapping or contiguous ranges merged. The input sequence must be sorted.
     */
    public static List<AbstractRange> union(List<? extends AbstractRange> ranges) {
        List<AbstractRange> collector = new ArrayList<>();
        for (var range : ranges) {
            if (collector.isEmpty()) {
                collector.add(range);
            } else {
                var last = collector.get(collector.size() - 1);
                collector.remove(collector.size() - 1);
                collector.addAll(union(last, range));
            }
        }
        return collector;
    }

    public static List<AbstractRange> union(AbstractRange left, AbstractRange right) {
        if (left.from() >= right.from() && left.to() <= right.to()) {
            return List.of(right);
        } else if (left.from() > right.to()) {
            if (right.to() + 1 == left.from()) return List.of(AbstractRange.of(right.from(), left.to()));
            else return List.of(right, left);
        } else if (left.to() < right.from()) {
            if (left.to() + 1 == right.from()) return List.of(AbstractRange.of(left.from(), right.to()));
            else return List.of(left, right);
        } else if (left.from() < right.from() && left.to() > right.to()) {
            return List.of(left);
        } else if (left.from() < right.from() && left.to() <= right.to()) {
            return List.of(AbstractRange.of(left.from(), right.to()));
        } else if (left.from() >= right.from() && left.to() > right.to()) {
            return List.of(AbstractRange.of(right.from(), left.to()));
        } else {
            throw new AssertionError();
        }
    }
}
