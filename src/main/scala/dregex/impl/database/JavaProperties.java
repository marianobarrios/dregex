package dregex.impl.database;

import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

public class JavaProperties {

    private static final Map<String, IntPredicate> properties = Map.ofEntries(
            Map.entry("javaLowerCase", Character::isLowerCase),
            Map.entry("javaUpperCase", Character::isUpperCase),
            Map.entry("javaAlphabetic", Character::isAlphabetic),
            Map.entry("javaIdeographic", Character::isIdeographic),
            Map.entry("javaTitleCase", Character::isTitleCase),
            Map.entry("javaDigit", Character::isDigit),
            Map.entry("javaDefined", Character::isDefined),
            Map.entry("javaLetter", Character::isLetter),
            Map.entry("javaLetterOrDigit", Character::isLetterOrDigit),
            Map.entry("javaJavaIdentifierStart", Character::isJavaIdentifierStart),
            Map.entry("javaJavaIdentifierPart", Character::isJavaIdentifierPart),
            Map.entry("javaUnicodeIdentifierStart", Character::isUnicodeIdentifierStart),
            Map.entry("javaUnicodeIdentifierPart", Character::isUnicodeIdentifierPart),
            Map.entry("javaIdentifierIgnorable", Character::isIdentifierIgnorable),
            Map.entry("javaSpaceChar", Character::isSpaceChar),
            Map.entry("javaWhitespace", Character::isWhitespace),
            Map.entry("javaISOControl", Character::isISOControl),
            Map.entry("javaMirrored", Character::isMirrored)
    );

    public static final Map<String, CharSet> charSets;

    static {
        charSets = new HashMap<>();
        for (var entry : properties.entrySet()) {
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
                        ranges.add(AbstractRange.create(rangeStart, codePoint - 1));
                        rangeStart = -1;
                    }
                }
            }
            if (rangeStart != -1) {
                ranges.add(AbstractRange.create(rangeStart, Character.MAX_CODE_POINT));
            }
            charSets.put(prop, new CharSet(ranges));
        }
    }

}
