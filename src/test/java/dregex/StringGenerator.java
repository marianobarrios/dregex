package dregex;

import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharSet;
import dregex.impl.tree.Disj;
import dregex.impl.tree.Juxt;
import dregex.impl.tree.Node;
import dregex.impl.tree.Rep;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generates, given a regex tree, sample strings that match the regex.
 */
class StringGenerator {

    static List<String> generate(Node regex, int maxAlternatives, int maxRepeat) {
        if (regex instanceof CharSet) {
            var set = (CharSet) regex;
            var gen = set.ranges.stream().map(range -> generate(range, maxAlternatives, maxRepeat).stream());
            return gen.flatMap(Function.identity()).collect(Collectors.toList());
        } else if (regex instanceof AbstractRange) {
            var range = (AbstractRange) regex;
            var length = Math.min(maxAlternatives, range.to() - range.from() + 1);
            return IntStream.range(0, length)
                    .mapToObj(i -> new String(Character.toChars(range.from() + i)))
                    .collect(Collectors.toList());
        } else if (regex instanceof Disj) {
            var disj = (Disj) regex;
            return disj.values.stream()
                    .flatMap(v -> generate(v, maxAlternatives, maxRepeat).stream())
                    .collect(Collectors.toList());
        } else if (regex instanceof Rep) {
            var rep = (Rep) regex;
            int max = rep.max.orElse(Integer.MAX_VALUE - 1);
            var count = 0;
            List<String> res = new ArrayList<>();
            for (int i = rep.min; i <= max; i++) {
                res.addAll(fixedRepeat(rep.value, maxAlternatives, maxRepeat, i));
                count += 1;
                if (count >= maxRepeat) {
                    break;
                }
            }
            return res;
        } else if (regex instanceof Juxt) {
            var juxt = (Juxt) regex;
            if (juxt.values.isEmpty()) {
                return List.of();
            } else if (juxt.values.size() == 1) {
                return generate(juxt.values.get(0), maxAlternatives, maxRepeat);
            } else {
                return generate(juxt.values.get(0), maxAlternatives, maxRepeat).stream()
                        .flatMap(
                                left -> generate(
                                                new Juxt(juxt.values.subList(1, juxt.values.size() - 1)),
                                                maxAlternatives,
                                                maxRepeat)
                                        .stream()
                                        .map(right -> left + right))
                        .collect(Collectors.toList());
            }
        } else {
            throw new RuntimeException("Unsupported node type: " + regex.getClass());
        }
    }

    static List<String> fixedRepeat(Node value, int maxAlternatives, int maxRepeat, int qtty) {
        /*
         * To avoid a too fast explosion of combinations, we limit the number of
         * alternatives and repetitions to 1 inside repetitions to all but one
         * instance.
         */
        if (qtty == 0) {
            return List.of();
        } else if (qtty == 1) {
            return generate(value, maxAlternatives, maxRepeat);
        } else {
            return generate(value, 1, 1).stream()
                    .flatMap(left -> fixedRepeat(value, maxAlternatives, maxRepeat, qtty - 1).stream()
                            .map(right -> left + right))
                    .collect(Collectors.toList());
        }
    }
}
