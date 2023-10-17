package dregex

import dregex.impl.Normalization
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

import java.time.Duration

/**
  * This test generates possible regex trees, and then generates strings
  * that should match those tree. This way we can automatically test as
  * many regex and strings as wanted. Note that this test is designed to
  * catch false negatives, but no false positives.
  */
class AutoTest  {

  private[this] val logger = LoggerFactory.getLogger(classOf[AutoTest])

  @Test
  def testGeneratedExamples() = {
    val generator = new TreeGenerator
    var totalTrees = 0
    var totalStrings = 0
    val start = System.nanoTime()
    for (tree <- generator.generate(maxDepth = 3)) {
      totalTrees += 1
      val regexString = tree.toRegex
      val regex = new CompiledRegex(regexString, tree, new Universe(java.util.List.of(tree), Normalization.NoNormalization))
      val strings = StringGenerator.generate(tree, maxAlternatives = 3, maxRepeat = 3)
      totalStrings += strings.size
      logger.debug("Testing: {}, generated: {}", regexString, strings.size)
      for (string <- strings) {
        assertTrue(regex.matches(string))
      }
    }
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(
      "Trees iteration took: {}; trees generated: {}; strings tested: {}",
      elapsed,
      Integer.valueOf(totalTrees),
      Integer.valueOf(totalStrings))
  }

}
