package dregex.impl.database;

import dregex.impl.RangeOps;
import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharRange;
import dregex.impl.tree.CharSet;
import dregex.impl.tree.Lit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UnicodePosixCharSets {

    private static final CharSet unicodeGraph = CharSet.fromCharSets(
                    UnicodeBinaryProperties.charSets.get("WHITE_SPACE"),
                    UnicodeGeneralCategories.charSets.get("Cc"),
                    UnicodeGeneralCategories.charSets.get("Cs"),
                    UnicodeGeneralCategories.charSets.get("Cn"))
            .complement();

    private static final CharSet unicodeBlank;

    static {
        List<AbstractRange> exceptions = new ArrayList<>();
        exceptions.addAll(UnicodeGeneralCategories.charSets.get("Zl").ranges);
        exceptions.addAll(UnicodeGeneralCategories.charSets.get("Zp").ranges);
        exceptions.add(new CharRange(0xA, 0xD));
        exceptions.add(new Lit(0x85));
        unicodeBlank =
                new CharSet(RangeOps.diff(UnicodeBinaryProperties.charSets.get("WHITE_SPACE").ranges, exceptions));
    }

    public static final Map<String, CharSet> charSets = Map.ofEntries(
            Map.entry("Lower", UnicodeBinaryProperties.charSets.get("LOWERCASE")),
            Map.entry("Upper", UnicodeBinaryProperties.charSets.get("UPPERCASE")),
            Map.entry("ASCII", PosixCharSets.charSets.get("ASCII")),
            Map.entry("Alpha", UnicodeBinaryProperties.charSets.get("ALPHABETIC")),
            Map.entry("Digit", UnicodeBinaryProperties.charSets.get("DIGIT")),
            Map.entry(
                    "Alnum",
                    CharSet.fromCharSets(
                            UnicodeBinaryProperties.charSets.get("ALPHABETIC"),
                            UnicodeBinaryProperties.charSets.get("DIGIT"))),
            Map.entry("Punct", UnicodeBinaryProperties.charSets.get("PUNCTUATION")),
            Map.entry("Graph", unicodeGraph),
            Map.entry(
                    "Print",
                    new CharSet(RangeOps.diff(
                            CharSet.fromCharSets(unicodeGraph, unicodeBlank).ranges,
                            UnicodeGeneralCategories.charSets.get("Cc").ranges))),
            Map.entry("Blank", unicodeBlank),
            Map.entry("Cntrl", UnicodeGeneralCategories.charSets.get("Cc")),
            Map.entry(
                    "XDigit",
                    CharSet.fromCharSets(
                            UnicodeGeneralCategories.charSets.get("Nd"),
                            UnicodeBinaryProperties.charSets.get("HEX_DIGIT"))),
            Map.entry("Space", UnicodeBinaryProperties.charSets.get("WHITE_SPACE")));

    public static final CharSet wordCharSet = CharSet.fromCharSets(
            UnicodeBinaryProperties.charSets.get("ALPHABETIC"),
            UnicodeGeneralCategories.charSets.get("Mn"),
            UnicodeGeneralCategories.charSets.get("Me"),
            UnicodeGeneralCategories.charSets.get("Mc"),
            UnicodeGeneralCategories.charSets.get("Mn"),
            UnicodeBinaryProperties.charSets.get("DIGIT"),
            UnicodeGeneralCategories.charSets.get("Pc"),
            UnicodeBinaryProperties.charSets.get("JOIN_CONTROL"));
}
