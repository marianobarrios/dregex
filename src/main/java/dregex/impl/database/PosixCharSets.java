package dregex.impl.database;

import dregex.impl.tree.CharRange;
import dregex.impl.tree.CharSet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import dregex.impl.tree.Lit;

public class PosixCharSets {

    private static final CharSet lower = new CharSet(new CharRange('a', 'z'));
    private static final CharSet upper = new CharSet(new CharRange('A', 'Z'));
    private static final CharSet alpha = CharSet.fromCharSets(lower, upper);
    public static final CharSet digit = new CharSet(new CharRange('0', '9'));
    private static final CharSet alnum = CharSet.fromCharSets(alpha, digit);
    private static final CharSet punct = new CharSet(
            "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".chars().mapToObj(ch -> new Lit(ch)).collect(Collectors.toList()));
    private static final CharSet graph = CharSet.fromCharSets(alnum, punct);
    public static final CharSet space = new CharSet(new Lit('\n'), new Lit('\t'),
            new Lit('\r'), new Lit('\f'), new Lit(' '), new Lit(0x0B));
    public static final CharSet wordChar = new CharSet(
            Stream.concat(alnum.ranges.stream(), Stream.of(new Lit('_'))).collect(Collectors.toList()));

    public static final Map<String, CharSet> charSets = Map.ofEntries(
            Map.entry("Lower", lower),
            Map.entry("Upper", upper),
            Map.entry("ASCII", new CharSet(new CharRange(0, 0x7F))),
            Map.entry("Alpha", alpha),
            Map.entry("Digit", digit),
            Map.entry("Alnum", alnum),
            Map.entry("Punct", punct),
            Map.entry("Graph", graph),
            Map.entry("Print", new CharSet(Stream.concat(graph.ranges.stream(),
                    Stream.of(new Lit(0x20))).collect(Collectors.toList()))),
            Map.entry("Blank", new CharSet(new Lit(0x20), new Lit('\t'))),
            Map.entry("Cntrl", new CharSet(new CharRange(0, 0x1F), new Lit(0x7F))),
            Map.entry("XDigit", new CharSet(Stream.concat(digit.ranges.stream(),
                    Stream.of(new CharRange('a', 'f'), new CharRange('A', 'F'))).collect(Collectors.toList()))),
            Map.entry("Space", space));
}
