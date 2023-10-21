package dregex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class EquivalenceTest {

    private boolean equiv(String left, String right) {
        var compiled = Regex.compile(java.util.List.of(left, right), Pattern.DOTALL);
        return compiled.get(0).equiv(compiled.get(1));
    }

    @Test
    public void testGrouping() {
        assertFalse(equiv("abd", "."));
        assertTrue(equiv("(a(b(c)d)e)", "abcde"));
        assertTrue(equiv("ab(c)", "(a(b)c)"));
        assertTrue(equiv("abc", "(abc)"));
        assertTrue(equiv("abc", "(?:abc)"));
        assertThrows(InvalidRegexException.class, () -> equiv("abc", "(?<name>abc)"));
    }

    @Test
    void testQuantifiers() {
        assertTrue(equiv("(a|b)+", "(a+|b+)+"));
        assertTrue(equiv("a+", "aa*"));
        assertTrue(equiv("a*a*", "a*"));
        assertTrue(equiv("a?a*", "a*"));
        assertTrue(equiv("(ab)+", "ab(ab)*"));
        assertTrue(equiv("a", "a{1}"));
        assertTrue(equiv("aa", "a{2}"));
        assertTrue(equiv("aaa", "a{3}"));
        assertTrue(equiv("a{0}", ""));
        assertTrue(equiv("a{1}", "a"));
        assertTrue(equiv("(a{2}){3}", "a{6}"));
        assertTrue(equiv("(a{2}){3}", "a{5}a"));
        assertFalse(equiv("(a{2}){3}", "a{5}"));
        assertTrue(equiv("a{2,3}", "aaa?"));
        assertTrue(equiv("a{2,3}", "a{2}a?"));
        assertTrue(equiv("a{0,3}", "a{0,2}a?"));
        assertTrue(equiv("a{3,}", "a{3}a*"));
        assertTrue(equiv("a{3,}", "a{2}a+"));
        assertTrue(equiv("a{3,}", "aaa+"));
    }

    @Test
    void testLookaround() {
        assertTrue(equiv("(?!a|b)(?!c).*", "(?!a|b|c).*"));
        assertTrue(equiv("a(?!b)", "a"));
        assertTrue(equiv("(?!b).|b", "."));
        assertTrue(equiv("a(?!b).|ab", "a."));
        assertTrue(equiv("a((?!b).|b)", "a."));
        assertTrue(equiv("a((?!b).|b)+", "a.+"));
        assertTrue(equiv("(a((?!b).|b))+", "(a.)+"));
        assertTrue(equiv("(?!a)(?!b).|c", "(?!a)(?!b)."));
        assertTrue(equiv("(?=.).*", ".+"));
        assertTrue(equiv("(?!a.*).+", "(?!a).+"));
        assertTrue(equiv("(?!a.?).+", "(?!a).+"));
        assertTrue(equiv("(?!a.{0,34}).+", "(?!a).+"));
        assertTrue(equiv("(?=a.*).+", "(?=a).+"));
        assertTrue(equiv("(?=a.?).+", "(?=a).+"));
        assertTrue(equiv("(?=a.{0,34}).+", "(?=a).+"));
        assertTrue(equiv("(?=.*).+", ".+"));
        assertTrue(equiv("(?=.?).+", ".+"));
        assertTrue(equiv("(?=.{0,34}).+", ".+"));
        assertTrue(equiv("(?=a*).+", ".+"));
        assertTrue(equiv("(?=a?).+", ".+"));
        assertTrue(equiv("(?=a{0,34}).+", ".+"));
    }

    @Test
    void testCharactedClasses() {
        assertTrue(equiv("[a]", "a"));
        assertTrue(equiv("a|b|c", "[abc]"));
        assertTrue(equiv("[abcdef]", "[a-f]"));
        assertTrue(equiv("[a-cdef]", "[a-f]"));
        assertTrue(equiv("[a-cd-f]", "[a-f]"));
    }

    @Test
    void testCombined() {
        assertTrue(equiv("(?!a|b).+", "[^ab].*"));
        assertTrue(equiv("(?=a|b).*", "[ab].*"));
    }

    @Test
    void testShortcutCharacterClasses() {
        assertTrue(equiv("\\d", "[0-9]"));
        assertTrue(equiv("\\w", "[a-zA-Z0-9_]"));
        assertTrue(equiv("\\s", "[ \\t\\n\\r\\f\\x{B}]"));
        assertTrue(equiv("\\d", "[\\d]"));
    }

    @Test
    void testPosixCharacterClasses() {
        assertTrue(equiv("\\p{Lower}", "[a-z]"));
        assertTrue(equiv("\\p{Upper}", "[A-Z]"));
        assertTrue(equiv("\\p{ASCII}", "[\\x{0}-\\x{7F}]"));
        assertTrue(equiv("\\p{Alpha}", "[\\p{Lower}\\p{Upper}]"));
        assertTrue(equiv("\\p{Digit}", "[0-9]"));
        assertTrue(equiv("\\p{Alnum}", "[\\p{Alpha}\\p{Digit}]"));
        assertTrue(equiv("\\p{Punct}", "[!\"#$%&'()*+,-./:;<=>?@[\\\\\\]\\^_`{|}~]"));
        assertTrue(equiv("\\p{Graph}", "[\\p{Alnum}\\p{Punct}]"));
        assertTrue(equiv("\\p{Print}", "[\\p{Graph}\\x{20}]"));
        assertTrue(equiv("\\p{Blank}", "[ \\t]"));
        assertTrue(equiv("\\p{Cntrl}", "[\\x{0}-\\x{1F}\\x{7F}]"));
        assertTrue(equiv("\\p{XDigit}", "[0-9a-fA-F]"));
        assertTrue(equiv("\\p{Space}", "[ \\t\\n\\x0B\\f\\r]"));
        assertTrue(equiv("\\p{Lower}", "[\\p{Lower}]"));
    }

    @Test
    void testDisjunctions() {
        var compiled = Regex.compile(java.util.List.of("a|b", "a", "b"));
        assertTrue(compiled.get(0).equiv((compiled.get(1).union(compiled.get(2)))));
    }

    @Test
    void testBlockQuotesAndLiteralFlag() {
        assertTrue(equiv("\\Q\\E", ""));
        assertTrue(equiv("\\Qabc\\E", "abc"));
        assertTrue(equiv("\\Qa*\\E", "a\\*"));
        assertTrue(equiv("(\\Qa*\\E)*", "(a\\*)*"));
        assertTrue(equiv("\\Q(\\E", "\\("));
        assertTrue(equiv("\\Q)\\E", "\\)"));
        assertTrue(equiv("(\\Q)a\\E)", "\\)a"));
        assertTrue(equiv("\\Q|\\E", "\\|"));

        var r = Regex.compile("a|bc", Pattern.LITERAL);
        assertTrue(r.matches("a|bc"));
    }
}
