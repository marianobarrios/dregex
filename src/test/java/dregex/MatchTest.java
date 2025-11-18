package dregex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dregex.impl.RegexImpl;
import dregex.impl.Universe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MatchTest {

    @Test
    void testCharacterClassesSimple() {

        assertFalse(RegexImpl.nullRegex(Universe.Empty).matchesAtLeastOne());

        {
            var r = Regex.compile("");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile(" ");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches(" "));
            assertFalse(r.matches("  "));
        }

        {
            var r = Regex.compile(".");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("aa"));
        }

        {
            var r = Regex.compile("[a-d]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("aa"));
            assertFalse(r.matches("x"));
        }

        {
            var r = Regex.compile("[^a]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("b"));
        }

        {
            var r = Regex.compile("[^ab]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertFalse(r.matches("b"));
            assertTrue(r.matches("c"));
        }

        {
            var r = Regex.compile("[ab-c]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("c"));
            assertFalse(r.matches("d"));
        }

        {
            var r = Regex.compile("[a-bc]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("c"));
            assertFalse(r.matches("d"));
        }

        {
            var r = Regex.compile("[^ab-c]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertFalse(r.matches("b"));
            assertFalse(r.matches("c"));
            assertTrue(r.matches("d"));
        }

        {
            var r = Regex.compile("[^a-bc]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertFalse(r.matches("b"));
            assertFalse(r.matches("c"));
            assertTrue(r.matches("d"));
        }
    }
    @Test
    void testInputStreamClassesSimple() throws IOException {

        assertFalse(RegexImpl.nullRegex(Universe.Empty).matchesAtLeastOne());

        {
            var r = Regex.compile("");
            assertTrue(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile(" ");
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream(" ".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("  ".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile(".");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("aa".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[a-d]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("aa".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[^a]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[^ab]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("c".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[ab-c]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("c".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("d".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[a-bc]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("c".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("d".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[^ab-c]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("c".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("d".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[^a-bc]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("c".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("d".getBytes(StandardCharsets.UTF_8))));
        }
    }

    @Test
    void testCharacterClassesSpecialCharactersInside() {

        // Special characters inside character classes
        assertTrue(Regex.compile("[.]").matches("."));
        assertTrue(Regex.compile("[(]").matches("("));
        assertTrue(Regex.compile("[)]").matches(")"));
        assertTrue(Regex.compile("[$]").matches("$"));
        assertTrue(Regex.compile("[[]").matches("["));
        assertTrue(Regex.compile("[\\]]").matches("]"));

        // Dash is interpreted literally inside character classes when it is the first or the last element

        {
            var r = Regex.compile("[-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("-"));
            assertFalse(r.matches("x"));
        }

        {
            var r = Regex.compile("[-a]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("-"));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("x"));
        }

        {
            var r = Regex.compile("[a-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("-"));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("x"));
        }

        {
            var r = Regex.compile("[-a-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("-"));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("x"));
        }

        {
            var r = Regex.compile("[^-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("-"));
            assertTrue(r.matches("a"));
        }

        {
            var r = Regex.compile("[^-a]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("-"));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("b"));
        }

        {
            var r = Regex.compile("[^a-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("-"));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("b"));
        }

        {
            var r = Regex.compile("[^-a-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("-"));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("b"));
        }
    }

    @Test
    void testInputStreamClassesSpecialCharactersInside() throws IOException {

        // Special characters inside input stream classes
        assertTrue(Regex.compile("[.]").matches(new ByteArrayInputStream(".".getBytes(StandardCharsets.UTF_8))));
        assertTrue(Regex.compile("[(]").matches(new ByteArrayInputStream("(".getBytes(StandardCharsets.UTF_8))));
        assertTrue(Regex.compile("[)]").matches(new ByteArrayInputStream(")".getBytes(StandardCharsets.UTF_8))));
        assertTrue(Regex.compile("[$]").matches(new ByteArrayInputStream("$".getBytes(StandardCharsets.UTF_8))));
        assertTrue(Regex.compile("[[]").matches(new ByteArrayInputStream("[".getBytes(StandardCharsets.UTF_8))));
        assertTrue(Regex.compile("[\\]]").matches(new ByteArrayInputStream("]".getBytes(StandardCharsets.UTF_8))));

        // Dash is interpreted literally inside character classes when it is the first or the last element

        {
            var r = Regex.compile("[-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("-".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("X".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[-a]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("-".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[a-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("-".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[-a-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("-".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[^-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("-".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[^-a]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("-".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[^a-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("-".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("[^-a-]");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("-".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8))));
        }
    }

    @Test
    void testCharacterClassesShorthand() {

        {
            var r = Regex.compile("\\d");
            assertFalse(r.matches(""));
            assertTrue(r.matches("0"));
            assertTrue(r.matches("9"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\w");
            assertFalse(r.matches(""));
            assertTrue(r.matches("0"));
            assertTrue(r.matches("9"));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("A"));
            assertTrue(r.matches("_"));
            assertFalse(r.matches(":"));
        }

        {
            var r = Regex.compile("\\s");
            assertFalse(r.matches(""));
            assertTrue(r.matches(" "));
            assertTrue(r.matches("\t"));
            assertTrue(r.matches("\n"));
            assertTrue(r.matches("\r"));
            assertTrue(r.matches("\f"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\D");
            assertFalse(r.matches(""));
            assertFalse(r.matches("0"));
            assertFalse(r.matches("9"));
            assertTrue(r.matches("a"));
        }

        {
            var r = Regex.compile("\\W");
            assertFalse(r.matches(""));
            assertFalse(r.matches("0"));
            assertFalse(r.matches("9"));
            assertFalse(r.matches("a"));
            assertFalse(r.matches("A"));
            assertFalse(r.matches("_"));
            assertTrue(r.matches(":"));
        }

        {
            var r = Regex.compile("\\S");
            assertFalse(r.matches(""));
            assertFalse(r.matches(" "));
            assertFalse(r.matches("\\t"));
            assertFalse(r.matches("\\n"));
            assertFalse(r.matches("\\r"));
            assertFalse(r.matches("\\f"));
            assertTrue(r.matches("a"));
        }

        {
            var compiled = Regex.compile(
                    java.util.List.of("\\S", "[\\S]", "[^\\s]", "\\s", "[\\s]", "[^\\S]", "."), Pattern.DOTALL);
            assertTrue(compiled.get(1).equiv(compiled.get(1)));
            assertTrue(compiled.get(1).equiv(compiled.get(2)));
            assertTrue(compiled.get(2).equiv(compiled.get(0)));
            assertTrue(compiled.get(4).equiv(compiled.get(4)));
            assertTrue(compiled.get(4).equiv(compiled.get(5)));
            assertTrue(compiled.get(5).equiv(compiled.get(3)));
            assertFalse(compiled.get(0).doIntersect(compiled.get(4)));
            assertTrue((compiled.get(0).union(compiled.get(4))).equiv(compiled.get(6)));
        }

        {
            var compiled = Regex.compile(
                    java.util.List.of("\\D", "[\\D]", "[^\\d]", "\\d", "[\\d]", "[^\\D]", "."), Pattern.DOTALL);
            assertTrue(compiled.get(1).equiv(compiled.get(1)));
            assertTrue(compiled.get(1).equiv(compiled.get(2)));
            assertTrue(compiled.get(2).equiv(compiled.get(0)));
            assertTrue(compiled.get(4).equiv(compiled.get(4)));
            assertTrue(compiled.get(4).equiv(compiled.get(5)));
            assertTrue(compiled.get(5).equiv(compiled.get(3)));
            assertFalse(compiled.get(0).doIntersect(compiled.get(4)));
            assertTrue((compiled.get(0).union(compiled.get(4)).equiv(compiled.get(6))));
        }

        {
            var compiled = Regex.compile(
                    java.util.List.of("\\W", "[\\W]", "[^\\w]", "\\w", "[\\w]", "[^\\W]", "."), Pattern.DOTALL);
            assertTrue(compiled.get(1).equiv(compiled.get(1)));
            assertTrue(compiled.get(1).equiv(compiled.get(2)));
            assertTrue(compiled.get(2).equiv(compiled.get(0)));
            assertTrue(compiled.get(4).equiv(compiled.get(4)));
            assertTrue(compiled.get(4).equiv(compiled.get(5)));
            assertTrue(compiled.get(5).equiv(compiled.get(3)));
            assertFalse(compiled.get(0).doIntersect(compiled.get(4)));
            assertTrue((compiled.get(0).union(compiled.get(4)).equiv(compiled.get(6))));
        }

        {
            var compiled = Regex.compile(java.util.List.of("\\d", "[^\\D\\W]"));
            assertTrue(compiled.get(0).equiv(compiled.get(1)));
        }
    }

    @Test
    void testInputStreamClassesShorthand() throws IOException{

        {
            var r = Regex.compile("\\d");
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("0".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("9".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("\\w");
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("0".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("9".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("A".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("_".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream(":".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("\\s");
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream(" ".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("\t".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("\r".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("\f".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("\\D");
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("0".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("9".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("\\W");
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("0".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("9".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("A".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("_".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream(":".getBytes(StandardCharsets.UTF_8))));
        }

        {
            var r = Regex.compile("\\S");
            assertFalse(r.matches(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream(" ".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("\\t".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("\\n".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("\\r".getBytes(StandardCharsets.UTF_8))));
            assertFalse(r.matches(new ByteArrayInputStream("\\f".getBytes(StandardCharsets.UTF_8))));
            assertTrue(r.matches(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8))));
        }

    }

    @Test
    void testQuantifiers() {

        {
            var r = Regex.compile("");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("a");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches("a"));
            assertFalse(r.matches("b"));
            assertFalse(r.matches("aa"));
        }

        {
            var r = Regex.compile("a*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertFalse(r.matches("aab"));
        }

        {
            var r = Regex.compile("a+");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertFalse(r.matches("aab"));
        }

        {
            var r = Regex.compile("a?");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("aa"));
            assertFalse(r.matches("aab"));
        }

        {
            var r = Regex.compile("(a{2})*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertFalse(r.matches("aaa"));
            assertTrue(r.matches("aaaa"));
        }

        {
            var r = Regex.compile("(a{2})+");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertFalse(r.matches("aaa"));
            assertTrue(r.matches("aaaa"));
        }

        {
            var r = Regex.compile("(a{2,3})*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("aaa"));
            assertTrue(r.matches("aaaa"));
            assertTrue(r.matches("aaaaa"));
            assertTrue(r.matches("aaaaaa"));
            assertTrue(r.matches("aaaaaaa"));
        }

        {
            var r = Regex.compile("a{0}");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("a{1}");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("aa"));
        }

        {
            var r = Regex.compile("a{2}");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertFalse(r.matches("aaa"));
        }

        {
            var r = Regex.compile("a{1,3}");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("aaa"));
            assertFalse(r.matches("aaaa"));
            assertFalse(r.matches("aab"));
        }

        {
            var r = Regex.compile("a{2,}");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("aaa"));
            assertFalse(r.matches("aab"));
        }

        {
            var r = Regex.compile("a{0,2}");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertFalse(r.matches("aaa"));
            assertFalse(r.matches("aab"));
        }

        // watch out!
        {
            var r = Regex.compile("a{,2}");
            assertTrue(r.matches("a{,2}"));
        }
    }

    @Test
    void testDisjunctions() {

        {
            var r = Regex.compile("a|b");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("b"));
            assertFalse(r.matches("c"));
            assertFalse(r.matches("aa"));
        }

        {
            var r = Regex.compile("ab|c");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertFalse(r.matches("b"));
            assertTrue(r.matches("c"));
            assertFalse(r.matches("aa"));
            assertTrue(r.matches("ab"));
            assertFalse(r.matches("abc"));
        }

        {
            var r = Regex.compile("(a|b)c");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("ac"));
            assertTrue(r.matches("bc"));
            assertFalse(r.matches("cc"));
            assertFalse(r.matches("aca"));
        }
    }

    @Test
    void testQuantifiersWithDisjunctions() {

        {
            var r = Regex.compile("((a|b)c)+");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("ac"));
            assertTrue(r.matches("bc"));
            assertFalse(r.matches("acc"));
            assertTrue(r.matches("acbc"));
        }

        {
            var r = Regex.compile("a*|b*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("aa"));
            assertFalse(r.matches("ab"));
            assertFalse(r.matches("ba"));
        }

        {
            var r = Regex.compile("a?|b*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("aa"));
            assertFalse(r.matches("ab"));
            assertFalse(r.matches("ba"));
        }

        {
            var r = Regex.compile("(a*|b*)|c");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("c"));
            assertFalse(r.matches("ac"));
            assertFalse(r.matches("bc"));
            assertFalse(r.matches("abc"));
        }

        {
            var r = Regex.compile("(a*|b*)*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("ab"));
            assertFalse(r.matches("abc"));
        }

        {
            var r = Regex.compile("(a*|b*)*|c");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("c"));
            assertFalse(r.matches("ac"));
            assertFalse(r.matches("bc"));
            assertFalse(r.matches("abc"));
        }

        {
            var r = Regex.compile("(a*|b*)+");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("bb"));
            assertTrue(r.matches("ab"));
            assertTrue(r.matches("bbbab"));
        }

        {
            var r = Regex.compile("(a+|b+)+");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("bb"));
            assertTrue(r.matches("ab"));
            assertTrue(r.matches("bbbab"));
        }

        {
            var r = Regex.compile("(a+|b+)*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("bb"));
            assertTrue(r.matches("ab"));
            assertTrue(r.matches("bbbab"));
        }

        {
            var r = Regex.compile("a+|b*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("bb"));
            assertFalse(r.matches("ab"));
            assertFalse(r.matches("bbbab"));
        }
    }

    @Test
    void testEscaping() {

        assertTrue(Regex.compile("\\B").matches("\\"));
        assertTrue(Regex.compile("\\\\").matches("\\"));

        assertTrue(Regex.compile("\\u0041").matches("A"));
        assertTrue(Regex.compile("\\x41").matches("A"));
        assertTrue(Regex.compile("\\x{41}").matches("A"));
        assertTrue(Regex.compile("\\x{000041}").matches("A"));
        assertTrue(Regex.compile("\\0101").matches("A"));

        assertTrue(Regex.compile("\\n").matches("\n"));
        assertTrue(Regex.compile("\\r").matches("\r"));
        assertTrue(Regex.compile("\\t").matches("\t"));
        assertTrue(Regex.compile("\\f").matches("\f"));
        assertTrue(Regex.compile("\\b").matches("\b"));

        {
            var r = Regex.compile("\\.");
            assertTrue(r.matches("."));
            assertFalse(r.matches("a"));
        }

        assertTrue(Regex.compile("\\+").matches("+"));
        assertTrue(Regex.compile("\\(").matches("("));
        assertTrue(Regex.compile("\\)").matches(")"));
    }

    @Test
    void testLookahead() {

        {
            var r = Regex.compile("(?!b)");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("a"));
            assertFalse(r.matches("b"));
        }

        {
            var r = Regex.compile("a(?!b).");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("ac"));
            assertFalse(r.matches("ab"));
        }

        {
            var r = Regex.compile("a(?!b).|other");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("ac"));
            assertFalse(r.matches("ab"));
            assertTrue(r.matches("other"));
        }

        {
            var r = Regex.compile("(a+(?!b).|other)+");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("aaa"));
            assertTrue(r.matches("ac"));
            assertFalse(r.matches("ab"));
            assertFalse(r.matches("aab"));
            assertTrue(r.matches("other"));
            assertTrue(r.matches("otherother"));
            assertTrue(r.matches("aaaother"));
            assertFalse(r.matches("aabother"));
            assertTrue(r.matches("aaotheraa"));
            assertFalse(r.matches("aabotherab"));
        }

        {
            var r = Regex.compile("([ax]+(?!b).|other)+");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("x"));
            assertTrue(r.matches("xa"));
            assertTrue(r.matches("xaa"));
            assertTrue(r.matches("xc"));
            assertFalse(r.matches("xb"));
            assertFalse(r.matches("xab"));
            assertTrue(r.matches("other"));
            assertTrue(r.matches("otherother"));
            assertTrue(r.matches("xaaother"));
            assertFalse(r.matches("xabother"));
            assertTrue(r.matches("xaotheraa"));
            assertFalse(r.matches("xabotherab"));
        }

        {
            var r = Regex.compile("a+(?!b).");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("ac"));
            assertFalse(r.matches("ab"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("aaa"));
            assertTrue(r.matches("aac"));
            assertFalse(r.matches("aab"));
        }

        {
            var r = Regex.compile(".*(?!a).*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("aaa"));
        }

        {
            var r = Regex.compile("(?!.?)");
            assertFalse(r.matchesAtLeastOne());
        }

        {
            var r = Regex.compile("(?=b)");
            assertFalse(r.matchesAtLeastOne());
        }

        {
            var r = Regex.compile("(?=.?)");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("(a|c)(?!b).*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches("ad"));
            assertFalse(r.matches("ab"));
            assertFalse(r.matches("cb"));
        }

        {
            var r = Regex.compile("[ac](?!b).*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches("ad"));
            assertFalse(r.matches("ab"));
            assertFalse(r.matches("cb"));
        }

        {
            var r = Regex.compile("(d|[ac])(?!b).*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches("ad"));
            assertFalse(r.matches("db"));
            assertFalse(r.matches("ab"));
            assertFalse(r.matches("cb"));
        }

        {
            var r = Regex.compile("(?!b)a");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("a"));
        }

        {
            var r = Regex.compile("a(?!b)");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches("a"));
        }

        {
            var r = Regex.compile("a+(?!b)");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches("a"));
            assertTrue(r.matches("aa"));
        }

        {
            var r = Regex.compile("(?!a)a");
            assertFalse(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("(?!.*)a");
            assertFalse(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("(?!a)b");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("b"));
        }

        {
            var r = Regex.compile("(?!a).");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("b"));
        }

        {
            var r = Regex.compile("(?=b)a");
            assertFalse(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertFalse(r.matches("b"));
        }

        {
            var r = Regex.compile("a(?=b)");
            assertFalse(r.matchesAtLeastOne());
        }

        {
            var r = Regex.compile("(?!b).*");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("b"));
            assertTrue(r.matches("ab"));
            assertFalse(r.matches("bb"));
        }

        {
            var r = Regex.compile("(?=b).*");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("b"));
            assertFalse(r.matches("ab"));
            assertTrue(r.matches("bb"));
        }

        {
            var r = Regex.compile("xxx(?=a|b)(?!c).*");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("xxxa"));
            assertTrue(r.matches("xxxb"));
            assertTrue(r.matches("xxxax"));
            assertTrue(r.matches("xxxbx"));
            assertFalse(r.matches("xxxc"));
            assertFalse(r.matches("xxxcx"));
            assertFalse(r.matches("xxxxb"));
        }

        {
            var r = Regex.compile("xxx(?=a|b).(?!c).*");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertTrue(r.matches("xxxay"));
            assertTrue(r.matches("xxxby"));
            assertTrue(r.matches("xxxay"));
            assertTrue(r.matches("xxxby"));
            assertFalse(r.matches("xxxyc"));
            assertFalse(r.matches("xxxycx"));
        }

        {
            var r = Regex.compile("xxx(?!a|b)(?!c).*");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("xxxa"));
            assertFalse(r.matches("xxxb"));
            assertFalse(r.matches("xxxax"));
            assertFalse(r.matches("xxxbx"));
            assertFalse(r.matches("xxxc"));
            assertFalse(r.matches("xxxcx"));
            assertTrue(r.matches("xxxxb"));
        }

        {
            var r = Regex.compile("xxx(?![ab])(?!c).*");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("xxxa"));
            assertFalse(r.matches("xxxb"));
            assertFalse(r.matches("xxxax"));
            assertFalse(r.matches("xxxbx"));
            assertFalse(r.matches("xxxc"));
            assertFalse(r.matches("xxxcx"));
            assertTrue(r.matches("xxxxb"));
        }

        {
            var r = Regex.compile("xxx(?!a|b)(?=.*)(?!c).*");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches(""));
            assertFalse(r.matches("xxxa"));
            assertFalse(r.matches("xxxb"));
            assertFalse(r.matches("xxxax"));
            assertFalse(r.matches("xxxbx"));
            assertFalse(r.matches("xxxc"));
            assertFalse(r.matches("xxxcx"));
            assertTrue(r.matches("xxxxb"));
        }

        assertFalse(Regex.compile("(?!.?).*").matchesAtLeastOne());
        assertFalse(Regex.compile("(?!.*).*").matchesAtLeastOne());
        assertFalse(Regex.compile("(?!.{0,10}).*").matchesAtLeastOne());
        assertFalse(Regex.compile("(?!a?).*").matchesAtLeastOne());
        assertFalse(Regex.compile("(?!a*).*").matchesAtLeastOne());
        assertFalse(Regex.compile("(?!a{0,10}).*").matchesAtLeastOne());

        {
            var r = Regex.compile("(?!a).|c.");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("a"));
            assertTrue(r.matches("b"));
            assertTrue(r.matches("c"));
            assertTrue(r.matches("cx"));
            assertFalse(r.matches("bx"));
        }

        {
            var r = Regex.compile("(a|aa)(?!b).+");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("a"));
            assertFalse(r.matches("ab"));
            assertTrue(r.matches("ac"));
            assertTrue(r.matches("aa"));
            assertTrue(r.matches("aac"));
            assertTrue(r.matches("aab"));
        }

        {
            var r = Regex.compile("(a|aa)(?!b)(c|cc)(?!d).*");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("a"));
            assertFalse(r.matches("ab"));
            assertTrue(r.matches("ac"));
            assertFalse(r.matches("aa"));
            assertTrue(r.matches("aac"));
            assertTrue(r.matches("acc"));
            assertTrue(r.matches("aacc"));
            assertTrue(r.matches("aaccd"));
            assertTrue(r.matches("aacce"));
        }

        {
            var r = Regex.compile("(?!bb)b");
            assertTrue(r.matches("b"));
        }

        {
            var r = Regex.compile("((?!bb)b)+");
            assertTrue(r.matches("b"));
            // true because lookahead cannot "escape" the expression it's in
            assertTrue(r.matches("bb"));
        }
    }

    @Test
    void testLookbehind() {

        {
            var r = Regex.compile("(?<!b)");
            assertTrue(r.matchesAtLeastOne());
            assertTrue(r.matches(""));
            assertFalse(r.matches("a"));
            assertFalse(r.matches("b"));
        }

        {
            var r = Regex.compile(".(?<!a)b");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("b"));
            assertTrue(r.matches("bb"));
            assertTrue(r.matches("cb"));
            assertFalse(r.matches("ab"));
        }

        {
            var r = Regex.compile("other|.(?<!a)b");
            assertTrue(r.matchesAtLeastOne());
            assertFalse(r.matches("b"));
            assertTrue(r.matches("bb"));
            assertTrue(r.matches("cb"));
            assertFalse(r.matches("ab"));
            assertTrue(r.matches("other"));
        }

        {
            var r = Regex.compile("a(?<!a)");
            assertFalse(r.matchesAtLeastOne());
        }

        {
            var r = Regex.compile("a(?<!a)|a");
            assertTrue(r.matches("a"));
        }

        {
            var r = Regex.compile("ong(?<!long)");
            assertTrue(r.matches("ong"));
        }

        {
            var r = Regex.compile("(a|b)(?<!a|b)");
            assertFalse(r.matchesAtLeastOne());
        }

        {
            var r = Regex.compile("[a-c](?<!a|b)");
            assertTrue(r.matches("c"));
        }
    }

    @Test
    void testSimpleFileInputStream() throws IOException {
        var compiledRegex = Regex.compile("(\\{\"time\"\\s*:\\s*\"(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z)\",\"severityText\"\\s*:\\s*(\"INFO\"|\"ERROR\"|\"WARNING\"),\"service.name\"\\s*:\\s*(\"auth-service\"|\"payment-service\"|\"storage-service\"),\"traceId\"\\s*:\\s*\"[a-z]{3}\\d{3}\",\"spanId\"\\s*:\\s*\"[a-z]{3}\\d{3}\"}\\s*)*");

        String resourceName = "log.json";

        // Load resource using the context class loader
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            assertTrue(compiledRegex.matches(inputStream));
        }
    }
}
