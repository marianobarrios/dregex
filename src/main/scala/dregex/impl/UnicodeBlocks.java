package dregex.impl;

import dregex.impl.tree.CharRange;
import dregex.impl.tree.CharSet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class UnicodeBlocks {

    private static final Map<String, UnicodeDatabaseReader.Range> ranges;

    static {
        try (var blocksFile = UnicodeBlocks.class.getResourceAsStream("/Blocks.txt")) {
            ranges = UnicodeDatabaseReader.getBlocks(new InputStreamReader(blocksFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<String, String> synonyms = Map.of(
            "Greek and Coptic", "Greek"
    );

    public static final Map<String, CharSet> unicodeBlocks;

    static {
        unicodeBlocks = new HashMap<>();
        for (var entry : ranges.entrySet()) {
            var block = entry.getKey();
            var range = entry.getValue();
            var charSet = CharSet.fromRange(new CharRange(range.from, range.to));
            unicodeBlocks.put(UnicodeDatabaseReader.canonicalizeBlockName(block), charSet);
        }
        for (var entry : synonyms.entrySet()) {
            var block = entry.getKey();
            var alias = entry.getValue();
            unicodeBlocks.put(UnicodeDatabaseReader.canonicalizeBlockName(alias),
                    unicodeBlocks.get(UnicodeDatabaseReader.canonicalizeBlockName(block)));
        }
    }

}
