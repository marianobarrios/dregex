package dregex.impl;

import static dregex.impl.CaseNormalization.NoNormalization;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestNormalization {

    @Test
    public void testNormalization() {
        assertEquals('A', NoNormalization.normalize('A'));
        assertEquals('a', CaseNormalization.LowerCase.normalize('A'));
        assertEquals('Á', CaseNormalization.LowerCase.normalize('Á'));
        assertEquals('á', CaseNormalization.UnicodeLowerCase.normalize('Á'));
    }
}
