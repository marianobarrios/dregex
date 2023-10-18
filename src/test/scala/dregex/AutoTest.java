package dregex;

import dregex.impl.Normalization;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
  * This test generates possible regex trees, and then generates strings
  * that should match those tree. This way we can automatically test as
  * many regex and strings as wanted. Note that this test is designed to
  * catch false negatives, but no false positives.
  */
class AutoTest  {

  private static final Logger logger = LoggerFactory.getLogger(AutoTest.class);

  @Test
  void testGeneratedExamples() {
    var generator = new TreeGenerator();
    var totalTrees = new AtomicInteger();
    var totalStrings = new AtomicInteger();
    var start = System.nanoTime();
    generator.generate(3).forEach(tree -> {
      totalTrees.incrementAndGet();
      var regexString = tree.toRegex();
      var regex = new CompiledRegex(regexString, tree, new Universe(List.of(tree), Normalization.NoNormalization));
      var strings = StringGenerator.generate(tree, 3, 3);
      totalStrings.addAndGet(strings.size());
      logger.debug("Testing: {}, generated: {}", regexString, strings.size());
      for (var string : strings) {
        assertTrue(regex.matches(string));
      }
    });
    var elapsed = Duration.ofNanos(System.nanoTime() - start);
    logger.debug("Trees iteration took: {}; trees generated: {}; strings tested: {}", elapsed, totalTrees, totalStrings);
  }

}
