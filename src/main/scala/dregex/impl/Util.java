package dregex.impl;

import java.util.HashSet;
import java.util.Set;

public class Util {

    public static <A> boolean doIntersect(Set<A> left, Set<A> right) {
        return right.stream().anyMatch(r -> left.contains(r));
    }

    public static <A> Set<A> union(Set<A> left, Set<A> right) {
        Set<A> ret = new HashSet<>(left);
        ret.addAll(right);
        return ret;
    }

}
