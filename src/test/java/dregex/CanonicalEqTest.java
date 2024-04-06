package dregex;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CanonicalEqTest {

    @Test
    void testCanonicalEquivalence() {
        assertTrue(Regex.compile("a\u030A", Pattern.CANON_EQ).matches("\u00E5"));
        assertTrue(Regex.compile("\u00E5", Pattern.CANON_EQ).matches("a\u030A"));
        assertTrue(Regex.compile("[\u00E5-\u00F0]", Pattern.CANON_EQ).matches("a\u030A"));
    }
}
