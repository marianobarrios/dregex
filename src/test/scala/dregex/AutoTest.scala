package dregex

import scala.collection.mutable.MultiMap
import dregex.impl.RegexTree
import org.scalatest.FunSuite
import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.Util
import dregex.impl.RegexParser
import org.scalatest.Assertions

/**
 * This test generates possible regex trees, and then generates strings
 * that should match those tree. This way we can automatically test as 
 * many regex and strings as wanted. Note that this test is designed to
 * catch false negatives, but no false positives.
 */
class AutoTest extends FunSuite with StrictLogging {

  test("generate examples") {
    val generator = new TreeGenerator
    var totalTrees = 0
    var totalStrings = 0
    val (_, elapsed) = Util.time {
      for (tree <- generator.generate(maxDepth = 3)) {
        totalTrees += 1
        val parsedRegex = new ParsedRegex(tree)
        val regexString = tree.toRegex
        val regex = new CompiledRegex(regexString, parsedRegex, new Universe(Seq(parsedRegex)))
        val strings = StringGenerator.generate(tree, maxAlternatives = 3, maxRepeat = 3)
        totalStrings += strings.size
        logger.debug(s"Testing: $regexString, generated: ${strings.size}")
        for (string <- strings) {
          assertResult(true)(regex.matches(string))
        }
      }
    }
    logger.debug(s"Trees iteration took: $elapsed; trees generated: $totalTrees; strings tested: $totalStrings")
  }

}