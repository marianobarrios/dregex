package dregex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CanonicalEqTest {

    @Test
    void testCanonicalEquivalence() {
        var nonCanonEqRegex = Regex.compile("\u00F6");
        assertTrue(nonCanonEqRegex.matches("\u00F6"));
        assertFalse(nonCanonEqRegex.matches("\u006F\u0308"));
        var canonEqRegex = Regex.compile("\u00F6", Pattern.CANON_EQ);
        assertTrue(canonEqRegex.matches("\u00F6"));
        assertTrue(canonEqRegex.matches("\u006F\u0308"));
    }
}
