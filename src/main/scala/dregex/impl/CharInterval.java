package dregex.impl;

import scala.math.Ordered;

import java.util.*;

public final class CharInterval implements AtomPart, Ordered<CharInterval> {

    public final UnicodeChar from;
    public final UnicodeChar to;

    public CharInterval(UnicodeChar from, UnicodeChar to) {
        if (from == null) {
            throw new NullPointerException("from is null");
        }
        if (to == null) {
            throw new NullPointerException("to is null");
        }

        if (from.compare(to) > 0) {
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
        return Objects.equals(from, that.from) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public int compare(CharInterval that) {
        return this.from.compare(that.from);
    }

    public String toString() {
        if (from.equals(to)) {
            return from.toString();
        } else {
            return String.format("[%s-%s]", from, to);
        }
    }

    public static Map<RegexTree.AbstractRange, List<CharInterval>> calculateNonOverlapping(List<RegexTree.AbstractRange> ranges) {
        Set<UnicodeChar> startSet = new HashSet<>();
        Set<UnicodeChar> endSet = new HashSet<>();
        for (var range : ranges) {
            startSet.add(range.from());
            if (range.from().compare(UnicodeChar.min()) > 0) {
                endSet.add(range.from().$minus(1));
            }
            endSet.add(range.to());
            if (range.to().compare(UnicodeChar.max()) < 0) {
                startSet.add(range.to().$plus(1));
            }
        }
        Map<RegexTree.AbstractRange, List<CharInterval>> ret = new HashMap<>();
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
