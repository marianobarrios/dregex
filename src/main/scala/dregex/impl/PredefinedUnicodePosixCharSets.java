package dregex.impl;

import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharRange;
import dregex.impl.tree.CharSet;
import dregex.impl.tree.Lit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PredefinedUnicodePosixCharSets {

    private static final CharSet unicodeGraph = CharSet
            .fromCharSets(
                    PredefinedCharSets.unicodeBinaryProperties().apply("WHITE_SPACE"),
                    UnicodeGeneralCategories.unicodeGeneralCategories.get("Cc"),
                    UnicodeGeneralCategories.unicodeGeneralCategories.get("Cs"),
                    UnicodeGeneralCategories.unicodeGeneralCategories.get("Cn")
            ).complement();

    private static final CharSet unicodeBlank;

    static {
        List<AbstractRange> exceptions = new ArrayList<>();
        exceptions.addAll(UnicodeGeneralCategories.unicodeGeneralCategories.get("Zl").ranges);
        exceptions.addAll(UnicodeGeneralCategories.unicodeGeneralCategories.get("Zp").ranges);
        exceptions.add(new CharRange(0xA, 0xD));
        exceptions.add(new Lit('\u0085'));
        unicodeBlank = new CharSet(
                RangeOps.diff(PredefinedCharSets.unicodeBinaryProperties().apply("WHITE_SPACE").ranges, exceptions));
    }

    public static final Map<String, CharSet> unicodePosixClasses = Map.ofEntries(
            Map.entry("Lower", PredefinedCharSets.unicodeBinaryProperties().apply("LOWERCASE")),
            Map.entry("Upper", PredefinedCharSets.unicodeBinaryProperties().apply("UPPERCASE")),
            Map.entry("ASCII", PredefinedPosixCharSets.classes.get("ASCII")),
            Map.entry("Alpha", PredefinedCharSets.unicodeBinaryProperties().apply("ALPHABETIC")),
            Map.entry("Digit", PredefinedCharSets.unicodeBinaryProperties().apply("DIGIT")),
            Map.entry("Alnum", CharSet.fromCharSets(
                    PredefinedCharSets.unicodeBinaryProperties().apply("ALPHABETIC"),
                    PredefinedCharSets.unicodeBinaryProperties().apply("DIGIT"))),
            Map.entry("Punct", PredefinedCharSets.unicodeBinaryProperties().apply("PUNCTUATION")),
            Map.entry("Graph", unicodeGraph),
            Map.entry("Print", new CharSet(
                    RangeOps.diff(
                            CharSet.fromCharSets(unicodeGraph, unicodeBlank).ranges,
                            UnicodeGeneralCategories.unicodeGeneralCategories.get("Cc").ranges))),
            Map.entry("Blank", unicodeBlank),
            Map.entry("Cntrl", UnicodeGeneralCategories.unicodeGeneralCategories.get("Cc")),
            Map.entry("XDigit", CharSet.fromCharSets(
                    UnicodeGeneralCategories.unicodeGeneralCategories.get("Nd"),
                    PredefinedCharSets.unicodeBinaryProperties().apply("HEX_DIGIT"))),
            Map.entry("Space", PredefinedCharSets.unicodeBinaryProperties().apply("WHITE_SPACE")));

    public static final CharSet unicodeWordChar = CharSet.fromCharSets(
            PredefinedCharSets.unicodeBinaryProperties().apply("ALPHABETIC"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Mn"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Me"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Mc"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Mn"),
            PredefinedCharSets.unicodeBinaryProperties().apply("DIGIT"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Pc"),
            PredefinedCharSets.unicodeBinaryProperties().apply("JOIN_CONTROL"));
}
