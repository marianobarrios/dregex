package dregex.impl;

import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharRange;
import dregex.impl.tree.CharSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnicodeBinaryProperties {

    public static final Map<String, CharSet> unicodeBinaryProperties;

    static {
        unicodeBinaryProperties = new HashMap<>();
        for (var entry : GeneralCategory.binaryProperties.entrySet()) {
            var prop = entry.getKey();
            var fn = entry.getValue();
            List<AbstractRange> ranges = new ArrayList<>();
            int rangeStart = -1;
            for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
                if (fn.test(codePoint)) {
                    if (rangeStart == -1) {
                        rangeStart = codePoint;
                    }
                } else {
                    if (rangeStart != -1) {
                        ranges.add(new CharRange(rangeStart, codePoint - 1));
                        rangeStart = -1;
                    }
                }
            }
            if (rangeStart != -1) {
                ranges.add(new CharRange(rangeStart, Character.MAX_CODE_POINT));
            }
            unicodeBinaryProperties.put(prop, new CharSet(ranges));
        }
    }

}
