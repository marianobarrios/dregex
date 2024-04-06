package dregex;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PerformanceTest {

    @Test
    void testSlowRegexs() {
        var start1 = System.nanoTime();
        var regexes = Regex.compile(List.of(
                "qwertyuiopasd",
                "/aaaaaaaa/(?!xxc)(?!xxd)(?!xxe)(?!xxf)(?!xxg)(?!xxh)[a-zA-Z0-9]{100}.*",
                "/aaaaaaaa/(?!x+c)(?!x+d)(?!x+e)(?!x+f)(?!x+g)(?!x+h)[a-zA-Z0-9]{100}.*",
                "/aaaaaaaa/(?!x+c|x+d|x+e|x+f|x+g|x+h)[a-zA-Z0-9]{100}.*",
                "/aaaaaaaa/(?!xxc)a(?!xxd)b(?!xxx)c(?!xxf)[a-zA-Z0-9]{100}.*", // disables lookahead combinations
                "/aaaaaaaa/(?!xxc|xxd|xxe|xxf|xxg|xxh)[a-zA-Z0-9]{100}.*",
                "/aaaaaaaa/(?!xxc.*)(?!xxd.*)(?!xxe.*)(?!xxf.*)(?!xxg.*)[a-zA-Z0-9]{100}.*"));
        var elapsed1 = Duration.ofNanos(System.nanoTime() - start1);
        System.out.println("compilation time: " + elapsed1);
        var start2 = System.nanoTime();
        for (var regex : regexes.subList(1, regexes.size() - 1)) {
            regex.doIntersect(regexes.get(0));
        }
        var elapsed2 = Duration.ofNanos(System.nanoTime() - start2);
        System.out.println("intersection time: " + elapsed2);
    }

    @Test
    void testLargeCharacterClasses() {
        var start = System.nanoTime();
        var regex = Regex.compile("[\\x{0}-\\x{10FFFA}]", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        System.out.printf("compilation time of %s: %s%n", regex, Duration.ofNanos(System.nanoTime() - start));
    }

    @Test
    void testUnicodeNormalization() {
        var start = System.nanoTime();
        var regex = Regex.compile("[\\x{0}-\\x{10FFFA}]", Pattern.CANON_EQ);
        System.out.printf("compilation time of %s: %s%n", regex, Duration.ofNanos(System.nanoTime() - start));
    }
}
