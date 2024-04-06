package dregex;

import static java.util.regex.Pattern.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CaseInsensitiveTest {

    @Test
    void testCaseInsensitive() {

        {
            var r = Regex.compile("a", CASE_INSENSITIVE);
            assertTrue(r.matches("A"));
            assertTrue(r.matches("a"));
        }

        {
            var r = Regex.compile("á", CASE_INSENSITIVE);
            assertFalse(r.matches("Á"));
            assertTrue(r.matches("á"));
        }

        {
            var r = Regex.compile("á", CASE_INSENSITIVE | UNICODE_CASE);
            assertTrue(r.matches("Á"));
            assertTrue(r.matches("á"));
        }

        {
            var r = Regex.compile("\\d", CASE_INSENSITIVE);
            assertTrue(r.matches("3"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\d", CASE_INSENSITIVE | UNICODE_CASE);
            assertTrue(r.matches("3"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("[ab]", CASE_INSENSITIVE);
            assertTrue(r.matches("a"));
            assertTrue(r.matches("b"));
            assertFalse(r.matches("c"));
        }

        {
            var r = Regex.compile("[ab]", CASE_INSENSITIVE);
            assertTrue(r.matches("A"));
            assertTrue(r.matches("B"));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("b"));
            assertFalse(r.matches("c"));
        }

        {
            var r = Regex.compile("[ab]", CASE_INSENSITIVE | UNICODE_CASE);
            assertTrue(r.matches("A"));
            assertTrue(r.matches("B"));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("b"));
            assertFalse(r.matches("c"));
        }

        {
            var r = Regex.compile("a|b", CASE_INSENSITIVE);
            assertTrue(r.matches("A"));
            assertTrue(r.matches("B"));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("b"));
            assertFalse(r.matches("c"));
        }

        {
            var r = Regex.compile("a|b", CASE_INSENSITIVE | UNICODE_CASE);
            assertTrue(r.matches("A"));
            assertTrue(r.matches("B"));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("b"));
            assertFalse(r.matches("c"));
        }

        assertFalse(Regex.compile("\\u0041").matches("a"));
        assertTrue(Regex.compile("\\u0041", CASE_INSENSITIVE).matches("a"));
    }

    @Test
    void testCaseInsensitiveRanges() {
        assertTrue(Regex.compile("[Y-b]").matches("Z"));
        assertFalse(Regex.compile("[Y-b]").matches("z"));
        assertTrue(Regex.compile("[Y-b]", CASE_INSENSITIVE).matches("Z"));
        assertTrue(Regex.compile("[Y-b]", CASE_INSENSITIVE).matches("z"));
    }

    @Test
    void testCaseInsensitiveLiterals() {
        assertFalse(Regex.compile("Abc", LITERAL).matches("abC"));
        assertTrue(Regex.compile("Abc", LITERAL | CASE_INSENSITIVE).matches("abC"));
    }
}
