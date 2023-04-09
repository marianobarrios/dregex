package dregex.impl;

import dregex.impl.tree.AbstractRange;
import scala.math.Ordered;

import java.util.*;

public final class CharInterval implements AtomPart, Ordered<CharInterval> {

    public final int from;
    public final int to;

    public CharInterval(int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException("from value cannot be larger than to");
        }
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharInterval that = (CharInterval) o;
        return from == that.from && to == that.to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public int compare(CharInterval that) {
        return Integer.compare(from, that.from);
    }

    public String toString() {
        if (from == to) {
            return Integer.toString(from);
        } else {
            return String.format("[%s-%s]", from, to);
        }
    }

    public static Map<AbstractRange, List<CharInterval>> calculateNonOverlapping(List<AbstractRange> ranges) {
        Set<Integer> startSet = new HashSet<>();
        Set<Integer> endSet = new HashSet<>();
        for (var range : ranges) {
            startSet.add(range.from());
            if (range.from() > Character.MIN_CODE_POINT) {
                endSet.add(range.from() - 1);
            }
            endSet.add(range.to());
            if (range.to() < Character.MAX_CODE_POINT) {
                startSet.add(range.to() + 1);
            }
        }
        Map<AbstractRange, List<CharInterval>> ret = new HashMap<>();
        for (var range : ranges) {
            var startCopySet = new java.util.TreeSet<>(startSet);
            var endCopySet = new java.util.TreeSet<>(endSet);
            var startSubSet = startCopySet.subSet(range.from(), true, range.to(), true);
            var endSubSet = endCopySet.subSet(range.from(), true, range.to(), true);
            assert startSubSet.size() == endSubSet.size();
            List<CharInterval> res = new ArrayList<>(startSubSet.size());
            do {
                var start = startSubSet.pollFirst();
                var end = endSubSet.pollFirst();
                res.add(new CharInterval(start, end));
            } while (!startSubSet.isEmpty());
            ret.put(range, res);
        }
        return ret;
    }

}
