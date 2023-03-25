package dregex

import dregex.impl.UnicodeChar
import dregex.impl.CharInterval
import dregex.impl.RegexTree.CharRange
import dregex.impl.RegexTree.AbstractRange
import org.scalatest.funsuite.AnyFunSuite
import scala.jdk.CollectionConverters._

class CharIntervalTest extends AnyFunSuite {

  implicit def intToUnicodeCharConversion(int: Int) = UnicodeChar(int)

  implicit def pairToRange(pair: (Int, Int)): CharRange = {
    pair match {
      case (from, to) => CharRange(UnicodeChar(from), UnicodeChar(to))
    }
  }

  implicit def pairToInterval(pair: (Int, Int)): CharInterval = {
    pair match {
      case (from, to) => new CharInterval(UnicodeChar(from), UnicodeChar(to))
    }
  }

  test("non-overlapping") {
    val ranges = Seq[AbstractRange]((10, 20), (21, 30), (0, 100), (9, 9), (10, 11), (9, 10), (10, 12), (17, 25))
    val nonOverlapping = CharInterval.calculateNonOverlapping(ranges.asJava)
    val expected = java.util.Map.of(
      CharRange(10, 20), java.util.List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11),
        new CharInterval(12, 12),
        new CharInterval(13, 16),
        new CharInterval(17, 20)),
      CharRange(21, 30), java.util.List.of(
        new CharInterval(21, 25),
        new CharInterval(26, 30)),
      CharRange(0, 100), java.util.List.of(
        new CharInterval(0, 8),
        new CharInterval(9, 9),
        new CharInterval(10, 10),
        new CharInterval(11, 11),
        new CharInterval(12, 12),
        new CharInterval(13, 16),
        new CharInterval(17, 20),
        new CharInterval(21, 25),
        new CharInterval(26, 30),
        new CharInterval(31, 100)),
      CharRange(9, 9), java.util.List.of(
        new CharInterval(9, 9)),
      CharRange(10, 11), java.util.List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11)),
      CharRange(9, 10), java.util.List.of(
        new CharInterval(9, 9),
        new CharInterval(10, 10)),
      CharRange(10, 12), java.util.List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11),
        new CharInterval(12, 12)),
      CharRange(17, 25), java.util.List.of(
        new CharInterval(17, 20),
        new CharInterval(21, 25))
    )
    assert(expected == nonOverlapping)
  }

}
