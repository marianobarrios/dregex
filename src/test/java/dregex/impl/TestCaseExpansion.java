package dregex.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class TestCaseExpansion {

    @Test
    public void test() {
        assertArrayEqualIgnoreOrder(new int[]{'A'}, CaseExpansion.NoExpansion.expand('A'));
        assertArrayEqualIgnoreOrder(new int[]{'a'}, CaseExpansion.NoExpansion.expand('a'));

        assertArrayEqualIgnoreOrder(new int[]{'a', 'A'}, CaseExpansion.Ascii.expand('a'));
        assertArrayEqualIgnoreOrder(new int[]{'a', 'A'}, CaseExpansion.Ascii.expand('A'));
        assertArrayEqualIgnoreOrder(new int[]{'á'}, CaseExpansion.Ascii.expand('á'));
        assertArrayEqualIgnoreOrder(new int[]{'Á'}, CaseExpansion.Ascii.expand('Á'));

        assertArrayEqualIgnoreOrder(new int[]{'a', 'A'}, CaseExpansion.Unicode.expand('a'));
        assertArrayEqualIgnoreOrder(new int[]{'a', 'A'}, CaseExpansion.Unicode.expand('A'));
        assertArrayEqualIgnoreOrder(new int[]{'á', 'Á'}, CaseExpansion.Unicode.expand('á'));
        assertArrayEqualIgnoreOrder(new int[]{'á', 'Á'}, CaseExpansion.Unicode.expand('Á'));
    }

    private static void assertArrayEqualIgnoreOrder(int[] left, int[] right) {
        left = left.clone();
        right = right.clone();
        Arrays.sort(left);
        Arrays.sort(right);
        assertArrayEquals(left, right);
    }
}
