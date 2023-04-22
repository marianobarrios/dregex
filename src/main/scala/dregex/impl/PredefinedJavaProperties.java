package dregex.impl;

import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharRange;
import dregex.impl.tree.CharSet;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

public class PredefinedJavaProperties {

    private static final Map<String, IntPredicate> properties;

    static {
        properties = new HashMap<>();
        for (var method : Character.class.getMethods()) {
            var name = method.getName();
            if (name.startsWith("is")) {
                var paramType = method.getParameters()[0].getType();
                if (paramType == int.class) {
                    var property = name.substring("is".length());
                    IntPredicate evaluationFn = codePoint -> {
                        try {
                            return (boolean) method.invoke(null, codePoint);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                    properties.put(property, evaluationFn);
                }
            }
        }
    }

    public static final Map<String, CharSet> javaClasses;

    static {
        javaClasses = new HashMap<>();
        for (var entry : properties.entrySet()) {
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
            javaClasses.put(prop, new CharSet(ranges));
        }
    }

}
