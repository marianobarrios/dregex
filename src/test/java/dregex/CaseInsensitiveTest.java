package dregex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CaseInsensitiveTest {

    @Test
    void testCaseInsensitive() {

        {
            var r = Regex.compile("a", Pattern.CASE_INSENSITIVE);
            assertTrue(r.matches("A"));
            assertTrue(r.matches("a"));
        }

        {
            var r = Regex.compile("á", Pattern.CASE_INSENSITIVE);
            assertFalse(r.matches("Á"));
            assertTrue(r.matches("á"));
        }

        {
            var r = Regex.compile("á", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
            assertTrue(r.matches("Á"));
            assertTrue(r.matches("á"));
        }

        {
            var r = Regex.compile("á", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            assertTrue(r.matches("Á"));
            assertTrue(r.matches("á"));
        }
    }
}
