package dregex;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerformanceTest {

    @Test
    void testSlowRegexs() {
        var start1 = System.nanoTime();
        var regexes = Regex.compile(List.of(
                "qwertyuiopasd",
                "/aaaaaa/(?!xxc)(?!xxd)(?!xxe)(?!xxf)(?!xxg)(?!xxh)[a-zA-Z0-9]{7}.*",
                "/aaaaaa/(?!x+c)(?!x+d)(?!x+e)(?!x+f)(?!x+g)(?!x+h)[a-zA-Z0-9]{7}.*",
                "/aaaaaa/(?!x+c|x+d|x+e|x+f|x+g|x+h)[a-zA-Z0-9]{7}.*",
                "/aaaaaa/(?!xxc)a(?!xxd)b(?!xxx)c(?!xxf)[a-zA-Z0-9]{7}.*", // disables lookahead combinations
                "/aaaaaa/(?!xxc|xxd|xxe|xxf|xxg|xxh)[a-zA-Z0-9]{7}.*",
                "/aaaaaa/(?!xxc.*)(?!xxd.*)(?!xxe.*)(?!xxf.*)(?!xxg.*)[a-zA-Z0-9]{7}.*"));
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
    void testLargeSharacterClasses() {
        var start = System.nanoTime();
        var regex = Regex.compile("[\\x{0}-\\x{10FFFF}]");
        var elapsed = Duration.ofNanos(System.nanoTime() - start);
        System.out.println("compilation time: " + elapsed);
    }
}
