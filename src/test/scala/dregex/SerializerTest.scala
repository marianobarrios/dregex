package dregex

import org.scalatest.FunSuite
import dregex.impl.RegexParser
import dregex.impl.Util
import org.slf4j.LoggerFactory

/**
 * Test the full cycle of serialization and parsing.
 */
class SerializerTest extends FunSuite {

  private[this] val logger = LoggerFactory.getLogger(classOf[SerializerTest])

  test("serialize") {
    val generator = new TreeGenerator
    var i = 0
    val elapsed = Util.time {
      for (tree <- generator.generate(maxDepth = 3)) {
        i += 1
        val canonicalTree = tree.canonical
        logger.trace("Generated tree: " + canonicalTree)
        val serialized = canonicalTree.toRegex
        logger.debug("Serialized: " + serialized)
        val reparsed = RegexParser.parse(serialized).canonical
        logger.trace("Reparsed: " + reparsed)
        val reserialized = reparsed.toRegex
        logger.trace("Reserialized: " + reserialized)
        assert(reparsed === canonicalTree)
        assert(serialized === reserialized)
      }
    }
    logger.debug(s"Trees iteration took: $elapsed; size: $i")
  }

}