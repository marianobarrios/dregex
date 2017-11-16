package dregex

import org.scalatest.FunSuite
import dregex.impl.Util
import org.slf4j.LoggerFactory

import scala.collection.immutable.Seq

/**
 * This test generates possible regex trees, and then generates strings
 * that should match those tree. This way we can automatically test as 
 * many regex and strings as wanted. Note that this test is designed to
 * catch false negatives, but no false positives.
 */
class AutoTest extends FunSuite {

  private[this] val logger = LoggerFactory.getLogger(classOf[AutoTest])

  test("generate examples") {
    val generator = new TreeGenerator
    var totalTrees = 0
    var totalStrings = 0
    val elapsed = Util.time {
      for (tree <- generator.generate(maxDepth = 3)) {
        totalTrees += 1
        val parsedRegex = new ParsedRegex(tree)
        val regexString = tree.toRegex
        val regex = new CompiledRegex(regexString, parsedRegex, new Universe(Seq(parsedRegex)))
        val strings = StringGenerator.generate(tree, maxAlternatives = 3, maxRepeat = 3)
        totalStrings += strings.size
        logger.debug("Testing: {}, generated: {}", regexString, strings.size)
        for (string <- strings) {
          assertResult(true)(regex.matches(string))
        }
      }
    }
    logger.debug("Trees iteration took: {}; trees generated: {}; strings tested: {}",
      elapsed, new java.lang.Integer(totalTrees), new java.lang.Integer(totalStrings))
  }

}