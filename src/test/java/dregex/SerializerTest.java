package dregex;

import dregex.impl.RegexParser;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
  * Test the full cycle of serialization and parsing.
  */
class SerializerTest {

  private static final Logger logger = LoggerFactory.getLogger(SerializerTest.class);

  @Test
  void testSerialize() {
    var generator = new TreeGenerator();
    var i = new AtomicInteger();
    var start = System.nanoTime();
    generator.generate(3).forEach(tree -> {
      i.incrementAndGet();
      var canonicalTree = tree.canonical();
      logger.trace("Generated tree: " + canonicalTree);
      var serialized = canonicalTree.toRegex();
      logger.debug("Serialized: " + serialized);
      var parserRegex = RegexParser.parse(serialized, new RegexParser.Flags());
      var reparsed = parserRegex.getTree().canonical();
      logger.trace("Reparsed: " + reparsed);
      var reserialized = reparsed.toRegex();
      logger.trace("Reserialized: " + reserialized);
      assertEquals(reparsed, canonicalTree);
      assertEquals(serialized, reserialized);
    });
    var elapsed = Duration.ofNanos(System.nanoTime() - start);
    logger.debug("Trees iteration took: {}; size: {}", elapsed, i);
  }

}
