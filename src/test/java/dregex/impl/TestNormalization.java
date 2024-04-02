package dregex.impl;

import static dregex.impl.Normalization.NoNormalization;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestNormalization {

    @Test
    public void testNormalization() {
        assertEquals("Abc", NoNormalization.normalize("Abc"));
        assertEquals("abc", Normalization.LowerCase.normalize("Abc"));
        assertEquals("Ábc", Normalization.LowerCase.normalize("Ábc"));
        assertEquals("ábc", Normalization.UnicodeLowerCase.normalize("Ábc"));
    }
}
