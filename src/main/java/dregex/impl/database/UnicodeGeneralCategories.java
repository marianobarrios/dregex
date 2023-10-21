package dregex.impl.database;

import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharSet;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnicodeGeneralCategories {

    private static final Map<String, List<UnicodeDatabaseReader.Range>> ranges;

    static {
        try (var scriptsFile = UnicodeScripts.class.getResourceAsStream("/DerivedGeneralCategory.txt")) {
            ranges = UnicodeDatabaseReader.getGeneralCategories(
                    new InputStreamReader(scriptsFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Map<String, CharSet> charSets;

    static {
        Map<String, CharSet> sets = new HashMap<>();
        for (var entry : ranges.entrySet()) {
            var block = entry.getKey();
            var ranges = entry.getValue();
            var chatSet = new CharSet(ranges.stream()
                    .map(range -> AbstractRange.of(range.from, range.to))
                    .collect(Collectors.toList()));
            sets.put(block, chatSet);
        }
        charSets = sets;
    }
}
