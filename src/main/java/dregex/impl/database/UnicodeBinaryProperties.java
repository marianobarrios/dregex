package dregex.impl.database;

import static java.lang.Character.*;

import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

public class UnicodeBinaryProperties {

    private static final Map<String, IntPredicate> binaryProperties = Map.ofEntries(
            Map.entry("ALPHABETIC", ch -> Character.isAlphabetic(ch)),
            Map.entry("DIGIT", ch -> Character.isDigit(ch)),
            Map.entry("LETTER", ch -> Character.isLetter(ch)),
            Map.entry("IDEOGRAPHIC", ch -> Character.isIdeographic(ch)),
            Map.entry("LOWERCASE", ch -> Character.isLowerCase(ch)),
            Map.entry("UPPERCASE", ch -> Character.isUpperCase(ch)),
            Map.entry("TITLECASE", ch -> Character.isTitleCase(ch)),
            Map.entry("WHITE_SPACE", ch -> isPropertyWhiteSpace(ch)),
            Map.entry("CONTROL", ch -> isCharacterControl(ch)),
            Map.entry("PUNCTUATION", ch -> isPropertyPunctuation(ch)),
            Map.entry("HEX_DIGIT", ch -> isPropertyHexDigit(ch)),
            Map.entry("ASSIGNED", ch -> Character.getType(ch) != Character.UNASSIGNED),
            Map.entry("NONCHARACTER_CODE_POINT", ch -> (ch & 0xfffe) == 0xfffe || (ch >= 0xfdd0 && ch <= 0xfdef)),
            Map.entry("ALNUM", ch -> Character.isAlphabetic(ch) || Character.isDigit(ch)),
            Map.entry("BLANK", ch -> isPropertyBlank(ch)),
            Map.entry("GRAPH", ch -> isPropertyGraph(ch)),
            Map.entry("PRINT", ch -> (isPropertyGraph(ch) || isPropertyBlank(ch)) && !isCharacterControl(ch)),
            Map.entry("JOIN_CONTROL", ch -> isPropertyJoinControl(ch)),
            Map.entry("WORD", ch -> isPropertyWord(ch)));

    private static boolean isPropertyWhiteSpace(int ch) {
        int type = Character.getType(ch);
        boolean isWhiteSpaceCat = type == SPACE_SEPARATOR || type == LINE_SEPARATOR || type == PARAGRAPH_SEPARATOR;
        return isWhiteSpaceCat || (ch >= 0x9 && ch <= 0xd) || (ch == 0x85);
    }

    private static boolean isCharacterControl(int ch) {
        return Character.getType(ch) == Character.CONTROL;
    }

    private static boolean isPropertyPunctuation(int ch) {
        int type = Character.getType(ch);
        return type == CONNECTOR_PUNCTUATION
                || type == DASH_PUNCTUATION
                || type == START_PUNCTUATION
                || type == END_PUNCTUATION
                || type == OTHER_PUNCTUATION
                || type == INITIAL_QUOTE_PUNCTUATION
                || type == FINAL_QUOTE_PUNCTUATION;
    }

    private static boolean isPropertyHexDigit(int ch) {
        return Character.isDigit(ch)
                || (ch >= 0x0030 && ch <= 0x0039)
                || (ch >= 0x0041 && ch <= 0x0046)
                || (ch >= 0x0061 && ch <= 0x0066)
                || (ch >= 0xFF10 && ch <= 0xFF19)
                || (ch >= 0xFF21 && ch <= 0xFF26)
                || (ch >= 0xFF41 && ch <= 0xFF46);
    }

    private static boolean isPropertyBlank(int ch) {
        return Character.getType(ch) == Character.SPACE_SEPARATOR || ch == 0x9; // \N{HT}
    }

    private static boolean isPropertyGraph(int ch) {
        int type = Character.getType(ch);
        return type == SPACE_SEPARATOR
                || type == LINE_SEPARATOR
                || type == PARAGRAPH_SEPARATOR
                || type == CONTROL
                || type == SURROGATE
                || type == UNASSIGNED;
    }

    private static boolean isPropertyJoinControl(int ch) {
        return ch == 0x200C || ch == 0x200D;
    }

    private static boolean isPropertyWord(int ch) {
        int type = Character.getType(ch);
        return Character.isAlphabetic(ch)
                || type == NON_SPACING_MARK
                || type == ENCLOSING_MARK
                || type == COMBINING_SPACING_MARK
                || type == DECIMAL_DIGIT_NUMBER
                || type == CONNECTOR_PUNCTUATION
                || isPropertyJoinControl(ch);
    }

    public static final Map<String, CharSet> charSets;

    static {
        charSets = new HashMap<>();
        for (var entry : binaryProperties.entrySet()) {
            var prop = entry.getKey();
            var fn = entry.getValue();
            List<AbstractRange> ranges = new ArrayList<>();
            int rangeStart = -1;
            for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
                if (fn.test(codePoint)) {
                    if (rangeStart == -1) {
                        rangeStart = codePoint;
                    }
                } else {
                    if (rangeStart != -1) {
                        ranges.add(AbstractRange.of(rangeStart, codePoint - 1));
                        rangeStart = -1;
                    }
                }
            }
            if (rangeStart != -1) {
                ranges.add(AbstractRange.of(rangeStart, Character.MAX_CODE_POINT));
            }
            charSets.put(prop, new CharSet(ranges));
        }
    }
}
