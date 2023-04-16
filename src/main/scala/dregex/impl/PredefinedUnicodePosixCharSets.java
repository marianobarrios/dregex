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
                    UnicodeBinaryProperties.unicodeBinaryProperties.get("WHITE_SPACE"),
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
                RangeOps.diff(UnicodeBinaryProperties.unicodeBinaryProperties.get("WHITE_SPACE").ranges, exceptions));
    }

    public static final Map<String, CharSet> unicodePosixClasses = Map.ofEntries(
            Map.entry("Lower", UnicodeBinaryProperties.unicodeBinaryProperties.get("LOWERCASE")),
            Map.entry("Upper", UnicodeBinaryProperties.unicodeBinaryProperties.get("UPPERCASE")),
            Map.entry("ASCII", PredefinedPosixCharSets.classes.get("ASCII")),
            Map.entry("Alpha", UnicodeBinaryProperties.unicodeBinaryProperties.get("ALPHABETIC")),
            Map.entry("Digit", UnicodeBinaryProperties.unicodeBinaryProperties.get("DIGIT")),
            Map.entry("Alnum", CharSet.fromCharSets(
                    UnicodeBinaryProperties.unicodeBinaryProperties.get("ALPHABETIC"),
                    UnicodeBinaryProperties.unicodeBinaryProperties.get("DIGIT"))),
            Map.entry("Punct", UnicodeBinaryProperties.unicodeBinaryProperties.get("PUNCTUATION")),
            Map.entry("Graph", unicodeGraph),
            Map.entry("Print", new CharSet(
                    RangeOps.diff(
                            CharSet.fromCharSets(unicodeGraph, unicodeBlank).ranges,
                            UnicodeGeneralCategories.unicodeGeneralCategories.get("Cc").ranges))),
            Map.entry("Blank", unicodeBlank),
            Map.entry("Cntrl", UnicodeGeneralCategories.unicodeGeneralCategories.get("Cc")),
            Map.entry("XDigit", CharSet.fromCharSets(
                    UnicodeGeneralCategories.unicodeGeneralCategories.get("Nd"),
                    UnicodeBinaryProperties.unicodeBinaryProperties.get("HEX_DIGIT"))),
            Map.entry("Space", UnicodeBinaryProperties.unicodeBinaryProperties.get("WHITE_SPACE")));

    public static final CharSet unicodeWordChar = CharSet.fromCharSets(
            UnicodeBinaryProperties.unicodeBinaryProperties.get("ALPHABETIC"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Mn"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Me"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Mc"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Mn"),
            UnicodeBinaryProperties.unicodeBinaryProperties.get("DIGIT"),
            UnicodeGeneralCategories.unicodeGeneralCategories.get("Pc"),
            UnicodeBinaryProperties.unicodeBinaryProperties.get("JOIN_CONTROL"));
}
