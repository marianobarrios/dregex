package dregex.impl;

import dregex.impl.tree.CharRange;
import dregex.impl.tree.CharSet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import dregex.impl.tree.Lit;

public class PredefinedPosixCharSets {

    public static CharSet lower = CharSet.fromRange(new CharRange('a', 'z'));
    public static CharSet upper = CharSet.fromRange(new CharRange('A', 'Z'));
    public static CharSet alpha = CharSet.fromCharSets(lower, upper);
    public static CharSet digit = CharSet.fromRange(new CharRange('0', '9'));
    public static CharSet alnum = CharSet.fromCharSets(alpha, digit);
    public static CharSet punct = new CharSet(
            "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".chars().mapToObj(ch -> new Lit(ch)).collect(Collectors.toList()));
    public static CharSet graph = CharSet.fromCharSets(alnum, punct);
    public static CharSet space = new CharSet(List.of(new Lit('\n'), new Lit('\t'),
            new Lit('\r'), new Lit('\f'), new Lit(' '), new Lit(0x0B)));
    public static CharSet wordChar = new CharSet(
            Stream.concat(alnum.ranges.stream(), Stream.of(new Lit('_'))).collect(Collectors.toList()));

    public static Map<String, CharSet> classes = Map.ofEntries(
            Map.entry("Lower", lower),
            Map.entry("Upper", upper),
            Map.entry("ASCII", CharSet.fromRange(new CharRange(0, 0x7F))),
            Map.entry("Alpha", alpha),
            Map.entry("Digit", digit),
            Map.entry("Alnum", alnum),
            Map.entry("Punct", punct),
            Map.entry("Graph", graph),
            Map.entry("Print", new CharSet(Stream.concat(graph.ranges.stream(),
                    Stream.of(new Lit(0x20))).collect(Collectors.toList()))),
            Map.entry("Blank", new CharSet(List.of(new Lit(0x20), new Lit('\t')))),
            Map.entry("Cntrl", new CharSet(List.of(new CharRange(0, 0x1F), new Lit(0x7F)))),
            Map.entry("XDigit", new CharSet(Stream.concat(digit.ranges.stream(),
                    Stream.of(new CharRange('a','f'), new CharRange('A', 'F'))).collect(Collectors.toList()))),
            Map.entry("Space", space));
}
