package dregex.impl;

import dregex.impl.tree.*;
import java.util.ArrayList;
import java.util.List;

public class CompilerHelper {

    /**
     * Optimization: combination of consecutive negative lookahead constructions
     * (?!a)(?!b)(?!c) gets combined to (?!a|b|c), which is faster to process.
     */
    public static Juxt combineNegLookaheads(Juxt juxt) {
        List<Node> newValues = new ArrayList<>();
        for (int i = 0; i < juxt.values.size(); i++) {
            if (i > 0 && juxt.values.get(i) instanceof Lookaround && juxt.values.get(i - 1) instanceof Lookaround) {
                var la1 = (Lookaround) juxt.values.get(i - 1);
                var la2 = (Lookaround) juxt.values.get(i);
                if (la1.dir == Direction.Ahead && la2.dir == Direction.Ahead) {
                    if (la1.cond == Condition.Negative && la2.cond == Condition.Negative) {
                        newValues.set(
                                newValues.size() - 1,
                                new Lookaround(Direction.Ahead, Condition.Negative, new Disj(la1.value, la2.value)));
                        continue;
                    }
                }
            }
            newValues.add(juxt.values.get(i));
        }
        return new Juxt(newValues);
    }

    public static int findLookaround(List<? extends Node> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) instanceof Lookaround) {
                return i;
            }
        }
        return -1;
    }
}
