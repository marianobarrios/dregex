package dregex;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaseSensitivityTest {

    @Test
    void testEmbeddedFlagPositions() {
        // OK
        Regex.compile("(?x)a");

        // flags in the middle
        assertThrows(InvalidRegexException.class, () -> Regex.compile(" (?x)a"));
        assertThrows(InvalidRegexException.class, () -> Regex.compile("(?x)a(?x)"));

        // unknown flag
        assertThrows(InvalidRegexException.class, () -> Regex.compile("(?w)a"));
    }

    @Test
    void testEmbeddedFlagsAscii() {
        {
            // (?i) in the first regex must not infect the second
            var regexes = Regex.compile(List.of("(?i)a", "b"));
            assertTrue(regexes.get(0).matches("a"));
            assertTrue(regexes.get(0).matches("A"));
            assertTrue(regexes.get(1).matches("b"));
            assertFalse(regexes.get(1).matches("B"));

            var union = regexes.get(0).union(regexes.get(1));
            assertTrue(union.matches("A"));
            assertTrue(union.matches("a"));
            assertTrue(union.matches("b"));
            assertFalse(union.matches("B"));
        }
        {
            var regexes = Regex.compile(List.of("(?i)a", "A"));
            assertTrue(regexes.get(0).matches("a"));
            assertTrue(regexes.get(0).matches("A"));
            assertFalse(regexes.get(1).matches("a"));
            assertTrue(regexes.get(1).matches("A"));

            var union = regexes.get(0).union(regexes.get(1));
            assertTrue(union.matches("a"));
            assertTrue(union.matches("A"));

            var intersection = regexes.get(0).intersect(regexes.get(1));
            assertFalse(intersection.matches("a"));
            assertTrue(intersection.matches("A"));

            var difference1 = regexes.get(0).diff(regexes.get(1));
            assertTrue(difference1.matches("a"));
            assertFalse(difference1.matches("A"));

            var difference2 = regexes.get(1).diff(regexes.get(0));
            assertFalse(difference2.matches("a"));
            assertFalse(difference2.matches("A"));
        }
        {
            var regexes = Regex.compile(List.of("(?i)a", "a|A"));
            assertTrue(regexes.get(0).equiv(regexes.get(1)));
        }
        {
            var regexes = Regex.compile(List.of("(?i)a", "[aA]"));
            assertTrue(regexes.get(0).equiv(regexes.get(1)));
        }
        {
            var regexes = Regex.compile(List.of("(?i)[ab]", "[abAB]"));
            assertTrue(regexes.get(0).equiv(regexes.get(1)));
        }
    }

    @Test
    void testEmbeddedFlagsUnicode() {
        {
            var regexes = Regex.compile(List.of("(?i)á", "Á"));
            assertTrue(regexes.get(0).matches("á"));
            assertFalse(regexes.get(0).matches("Á"));
            assertFalse(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
        }
        {
            var regexes = Regex.compile(List.of("(?iu)á", "Á"));
            assertTrue(regexes.get(0).matches("á"));
            assertTrue(regexes.get(0).matches("Á"));
            assertFalse(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
        }
        {
            // The unicode flag alone does nothing
            var regexes = Regex.compile(List.of("(?u)á", "Á"));
            assertTrue(regexes.get(0).matches("á"));
            assertFalse(regexes.get(0).matches("Á"));
            assertFalse(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
        }
    }

    @Test
    void testOuterFlag() {
        {
            var regexes = Regex.compile(List.of("a", "A"), Pattern.CASE_INSENSITIVE);
            assertTrue(regexes.get(0).matches("a"));
            assertTrue(regexes.get(0).matches("A"));
            assertTrue(regexes.get(1).matches("a"));
            assertTrue(regexes.get(1).matches("A"));
            assertTrue(regexes.get(0).equiv(regexes.get(1)));

            var intersection = regexes.get(0).intersect(regexes.get(1));
            assertTrue(intersection.matches("a"));
            assertTrue(intersection.matches("A"));
        }
        {
            var regexes = Regex.compile(List.of("á", "Á"), Pattern.CASE_INSENSITIVE);
            assertTrue(regexes.get(0).matches("á"));
            assertFalse(regexes.get(0).matches("Á"));
            assertFalse(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
            assertFalse(regexes.get(0).equiv(regexes.get(1)));
        }
        {
            var regexes = Regex.compile(List.of("á", "Á"), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            assertTrue(regexes.get(0).matches("á"));
            assertTrue(regexes.get(0).matches("Á"));
            assertTrue(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
            assertTrue(regexes.get(0).equiv(regexes.get(1)));
        }
        {
            // UNICODE_CASE alone does not do anything
            var regexes = Regex.compile(List.of("á", "Á"), Pattern.UNICODE_CASE);
            assertTrue(regexes.get(0).matches("á"));
            assertFalse(regexes.get(0).matches("Á"));
            assertFalse(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
            assertFalse(regexes.get(0).equiv(regexes.get(1)));
        }
    }

    @Test
    void testEmbeddedAndOuterFlag() {
        {
            var regexes = Regex.compile(List.of("(?i)a", "A"), Pattern.CASE_INSENSITIVE);
            assertTrue(regexes.get(0).matches("a"));
            assertTrue(regexes.get(0).matches("A"));
            assertTrue(regexes.get(1).matches("a"));
            assertTrue(regexes.get(1).matches("A"));
            assertTrue(regexes.get(0).equiv(regexes.get(1)));
        }
        {
            var regexes = Regex.compile(List.of("(?i)á", "Á"), Pattern.CASE_INSENSITIVE);
            assertTrue(regexes.get(0).matches("á"));
            assertFalse(regexes.get(0).matches("Á"));
            assertFalse(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
            assertFalse(regexes.get(0).equiv(regexes.get(1)));
        }
        {
            var regexes = Regex.compile(List.of("(?iu)á", "Á"), Pattern.CASE_INSENSITIVE);
            assertTrue(regexes.get(0).matches("á"));
            assertTrue(regexes.get(0).matches("Á"));
            assertFalse(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
            assertFalse(regexes.get(0).equiv(regexes.get(1)));
        }
        {
            var regexes = Regex.compile(List.of("(?i)á", "Á"), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            assertTrue(regexes.get(0).matches("á"));
            assertTrue(regexes.get(0).matches("Á"));
            assertTrue(regexes.get(1).matches("á"));
            assertTrue(regexes.get(1).matches("Á"));
            assertTrue(regexes.get(0).equiv(regexes.get(1)));
        }
    }
}
