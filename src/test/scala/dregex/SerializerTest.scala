package dregex

import dregex.impl.RegexParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

import java.time.Duration

/**
  * Test the full cycle of serialization and parsing.
  */
class SerializerTest {

  private[this] val logger = LoggerFactory.getLogger(classOf[SerializerTest])

  @Test
  def testSerialize() = {
    val generator = new TreeGenerator
    var i = 0
    val start = System.nanoTime()
    for (tree <- generator.generate(maxDepth = 3)) {
      i += 1
      val canonicalTree = tree.canonical
      logger.trace("Generated tree: " + canonicalTree)
      val serialized = canonicalTree.toRegex
      logger.debug("Serialized: " + serialized)
      val parserRegex = RegexParser.parse(serialized, new RegexParser.Flags())
      val reparsed = parserRegex.getTree.canonical
      logger.trace("Reparsed: " + reparsed)
      val reserialized = reparsed.toRegex
      logger.trace("Reserialized: " + reserialized)
      assertEquals(reparsed, canonicalTree)
      assertEquals(serialized, reserialized)
    }
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(s"Trees iteration took: $elapsed; size: $i")
  }

}
