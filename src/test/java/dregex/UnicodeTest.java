package dregex;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dregex.impl.database.UnicodeBlocks;
import dregex.impl.database.UnicodeScripts;
import java.lang.Character.UnicodeBlock;
import java.lang.Character.UnicodeScript;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UnicodeTest {

    private static final Logger logger = LoggerFactory.getLogger(UnicodeTest.class);

    @Test
    void testAstralPlanes() {

        {
            var r = Regex.compile(".");
            assertTrue(r.matches("a"));
            assertTrue(r.matches("𐐷"));
            assertTrue(r.matches("\uD801\uDC37"));
        }

        {
            var r = Regex.compile("𐐷");
            assertFalse(r.matches("a"));
            assertTrue(r.matches("𐐷"));
            assertTrue(r.matches("\uD801\uDC37"));
        }

        {
            var r = Regex.compile("𐐷", Pattern.LITERAL);
            assertFalse(r.matches("a"));
            assertTrue(r.matches("𐐷"));
            assertTrue(r.matches("\uD801\uDC37"));
        }
    }

    @Test
    void testEscapes() {

        /*
         * Note that Unicode escaping still happens at the source code level even inside triple quotes, so
         * have to double escape in those cases.
         */

        assertTrue(Regex.compile("\\x41").matches("A"));
        assertTrue(Regex.compile("\\u0041").matches("A"));
        assertTrue(Regex.compile("\\x{41}").matches("A"));
        assertTrue(Regex.compile("\\x{10437}").matches("𐐷"));

        // double Unicode escaping
        {
            var r = Regex.compile("\\uD801\\uDC37");
            assertTrue(r.matches("𐐷"));
        }

        // high surrogate alone, works like a normal character
        {
            var r = Regex.compile("\\uD801");
            assertFalse(r.matches("A"));
            assertTrue(r.matches("\uD801"));
        }

        // high surrogate followed by normal char, works like two normal characters
        {
            var r = Regex.compile("\\uD801\\u0041");
            assertFalse(r.matches("A"));
            assertTrue(r.matches("\uD801\u0041"));
            assertTrue(r.matches("\uD801" + "\u0041"));
        }
    }

    @Test
    void testBlocks() {

        {
            var r = Regex.compile("\\p{InGreek}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("Ω"));
            assertFalse(r.matches("z"));
        }

        {
            var r = Regex.compile("\\p{InGREEK}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{InGreek and Coptic}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{block=Greek}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{blk=Greek}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
        }

        /*
         * Exhaustively test all combinations of Unicode blocks and code points against
         * the java.util.regex implementation.
         */
        for (var block : UnicodeBlocks.charSets.keySet().stream().sorted().collect(Collectors.toList())) {

            boolean blockExistsInJava;
            try {
                Character.UnicodeBlock.forName(block);
                blockExistsInJava = true;
            } catch (IllegalArgumentException e) {
                blockExistsInJava = false;
            }

            if (blockExistsInJava) {
                logger.debug("testing Unicode block {}...", block);
                // a regex that matches any character of the block
                var regexString = String.format("\\p{block=%s}", block);
                var regex = Regex.compile(regexString);
                var javaRegex = java.util.regex.Pattern.compile(regexString);
                for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint++) {

                    if (codePoint >= 0x18D80 && codePoint <= 0x18D8F) {
                        // Unicode 14 removed mistakenly added characters at the end of the "Tangut Supplement" block
                        // Excluding them for testing in old JVM versions, that can have the old range.
                        // Source: https://www.unicode.org/versions/Unicode14.0.0/erratafixed.html
                        break;
                    }

                    var codePointAsString = new String(new int[] {codePoint}, 0, 1);
                    if (javaRegex.matcher(codePointAsString).matches()) {
                        assertTrue(
                                regex.matches(codePointAsString),
                                String.format(
                                        "- block: %s; java block: %s; code point: 0x%04X",
                                        block, UnicodeBlock.of(codePoint), codePoint));
                    }
                }
            } else {
                logger.debug("skipping Unicode block {} as it's not present in the current Java version", block);
            }
        }
    }

    @Test
    void testScripts() {

        {
            var r = Regex.compile("\\p{IsGreek}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
            assertTrue(r.matches("Ω"));
            assertFalse(r.matches("z"));
        }

        {
            var r = Regex.compile("\\p{IsGREEK}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{IsGREEK}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{script=GREK}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{sc=Greek}");
            assertTrue(r.matches("α"));
            assertFalse(r.matches("a"));
        }

        /*
         * Exhaustively test all combinations of Unicode scripts and code points against
         * the java.util.regex implementation.
         */
        for (var script : UnicodeScripts.chatSets.keySet().stream().sorted().collect(Collectors.toList())) {

            boolean scriptExistsInJava;
            try {
                Character.UnicodeScript.forName(script);
                scriptExistsInJava = true;
            } catch (IllegalArgumentException e) {
                scriptExistsInJava = false;
            }

            if (scriptExistsInJava) {
                logger.debug("testing Unicode script {}...", script);
                // a regex that matches any character of the block
                var regexString = String.format("\\p{script=%s}", script);
                var regex = Regex.compile(regexString);
                var javaRegex = java.util.regex.Pattern.compile(regexString);
                for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint++) {

                    // A few code points were removed from scripts as Java versions evolved, ignore them
                    if (codePoint >= 0x0951 && codePoint <= 0x0954) {
                        // these 4 characters were moved from Devanagari to Inherited script,
                        // so we have to exclude them from the tests for them to work across different Java versions.
                        // Source: https://unicode.org/mail-arch/unicode-ml/y2002-m12/0053.html
                        break;
                    } else if (codePoint == 0xA9CF) {
                        // this character was apparently moved from Javanese to Common script,
                        // so we have to exclude them from the tests for them to work across different Java versions.
                        break;
                    } else if (codePoint == 0xA92E) {
                        // this character was apparently moved from Kali to Common script,
                        // so we have to exclude them from the tests for them to work across different Java versions.
                        break;
                    }

                    // As code points are added to scripts, this test count fail, so we not assert when the
                    // code points are not assigned in Java (it can just be an old version).
                    var javaScript = UnicodeScript.of(codePoint);
                    if (javaScript == UnicodeScript.UNKNOWN || javaScript == UnicodeScript.COMMON) {
                        break;
                    }

                    var codePointAsString = new String(new int[] {codePoint}, 0, 1);
                    if (javaRegex.matcher(codePointAsString).matches()) {
                        assertTrue(
                                regex.matches(codePointAsString),
                                String.format(
                                        "- script: %s; java script: %s; code point: 0x%04X",
                                        script, UnicodeScript.of(codePoint), codePoint));
                    }
                }
            } else {
                logger.debug("skipping Unicode script {} as it's not present in the current Java version", script);
            }
        }
    }

    @Test
    void testGeneralCategories() {

        {
            var r = Regex.compile("\\p{Lu}");
            assertTrue(r.matches("A"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{IsLu}");
            assertTrue(r.matches("A"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{general_category=Lu}");
            assertTrue(r.matches("A"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{gc=Lu}");
            assertTrue(r.matches("A"));
            assertFalse(r.matches("a"));
        }

        {
            var r = Regex.compile("\\p{general_category=L}");
            assertTrue(r.matches("A"));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("-"));
        }
    }

    @Test
    void testBinaryProperties() {

        {
            var r = Regex.compile("\\p{IsAlphabetic}");
            assertTrue(r.matches("A"));
            assertTrue(r.matches("a"));
            assertFalse(r.matches("*"));
        }

        {
            var r = Regex.compile("\\p{IsHex_Digit}");
            assertTrue(r.matches("f"));
            assertFalse(r.matches("g"));
        }
    }

    @Test
    void testLinebreak() {

        {
            var r = Regex.compile("\\R");
            assertTrue(r.matches("\n"));
            assertTrue(r.matches(new String(new char[] {0xA})));
            assertTrue(r.matches(new String(new char[] {0x2029})));
            assertFalse(r.matches(new String(new char[] {0xA, 0xD})));
            assertTrue(r.matches(new String(new char[] {0xD, 0xA})));
        }
    }

    @Test
    void testJavaCategories() {

        {
            var r = Regex.compile("\\p{javaLowerCase}");
            assertTrue(r.matches("a"));
            assertFalse(r.matches("A"));
        }
    }
}
