package dregex.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dregex.impl.RegexTree.CharSet$;
import static dregex.impl.RegexTree.CharSet;
import static dregex.impl.RegexTree.Lit;
import static dregex.impl.RegexTree.CharRange;

public class PredefinedPosixCharSets {

    public static CharSet lower = CharSet$.MODULE$.fromRange(new CharRange('a', 'z'));
    public static CharSet upper = CharSet$.MODULE$.fromRange(new CharRange('A', 'Z'));
    public static CharSet alpha = CharSet$.MODULE$.fromCharSetsJava(List.of(lower, upper));
    public static CharSet digit = CharSet$.MODULE$.fromRange(new CharRange('0', '9'));
    public static CharSet alnum = CharSet$.MODULE$.fromCharSetsJava(List.of(alpha, digit));
    public static CharSet punct = CharSet$.MODULE$.fromJava(
            "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".chars().mapToObj(ch -> new Lit(ch)).collect(Collectors.toList()));
    public static CharSet graph = CharSet$.MODULE$.fromCharSetsJava(List.of(alnum, punct));
    public static CharSet space = CharSet$.MODULE$.fromJava(List.of(new Lit('\n'), new Lit('\t'),
            new Lit('\r'), new Lit('\f'), new Lit(' '), new Lit(0x0B)));
    public static CharSet wordChar = CharSet$.MODULE$.fromJava(
            Stream.concat(alnum.javaRanges().stream(), Stream.of(new Lit('_'))).collect(Collectors.toList()));

    public static Map<String, CharSet> classes = Map.ofEntries(
            Map.entry("Lower", lower),
            Map.entry("Upper", upper),
            Map.entry("ASCII", CharSet$.MODULE$.fromRange(new CharRange(0, 0x7F))),
            Map.entry("Alpha", alpha),
            Map.entry("Digit", digit),
            Map.entry("Alnum", alnum),
            Map.entry("Punct", punct),
            Map.entry("Graph", graph),
            Map.entry("Print", CharSet$.MODULE$.fromJava(Stream.concat(graph.javaRanges().stream(),
                    Stream.of(new Lit(0x20))).collect(Collectors.toList()))),
            Map.entry("Blank", CharSet$.MODULE$.fromJava(List.of(new Lit(0x20), new Lit('\t')))),
            Map.entry("Cntrl", CharSet$.MODULE$.fromJava(List.of(new CharRange(0, 0x1F), new Lit(0x7F)))),
            Map.entry("XDigit", CharSet$.MODULE$.fromJava(Stream.concat(digit.javaRanges().stream(),
                    Stream.of(new CharRange('a','f'), new CharRange('A', 'F'))).collect(Collectors.toList()))),
            Map.entry("Space", space));
}
