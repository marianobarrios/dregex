package dregex;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmbeddedFlagTest {

    @Test
    void testEmbeddedFlags() {

        // OK
        Regex.compile("(?x)a");

        // flags in the middle
        assertThrows(InvalidRegexException.class, () -> Regex.compile(" (?x)a"));
        assertThrows(InvalidRegexException.class, () -> Regex.compile("(?x)a(?x)"));

        // unknown flag
        assertThrows(InvalidRegexException.class, () -> Regex.compile("(?w)a"));
    }
}
