package dregex

import dregex.impl.CharInterval
import dregex.impl.tree.CharRange
import org.scalatest.funsuite.AnyFunSuite
import dregex.impl.tree.AbstractRange
import scala.jdk.CollectionConverters._

class CharIntervalTest extends AnyFunSuite {

  implicit def pairToRange(pair: (Int, Int)): CharRange = {
    pair match {
      case (from, to) => new CharRange(from, to)
    }
  }

  implicit def pairToInterval(pair: (Int, Int)): CharInterval = {
    pair match {
      case (from, to) => new CharInterval(from, to)
    }
  }

  test("non-overlapping") {
    val ranges = Seq[AbstractRange]((10, 20), (21, 30), (0, 100), (9, 9), (10, 11), (9, 10), (10, 12), (17, 25))
    val nonOverlapping = CharInterval.calculateNonOverlapping(ranges.asJava)
    val expected = java.util.Map.of(
      new CharRange(10, 20), java.util.List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11),
        new CharInterval(12, 12),
        new CharInterval(13, 16),
        new CharInterval(17, 20)),
      new CharRange(21, 30), java.util.List.of(
        new CharInterval(21, 25),
        new CharInterval(26, 30)),
      new CharRange(0, 100), java.util.List.of(
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
      new CharRange(9, 9), java.util.List.of(
        new CharInterval(9, 9)),
      new CharRange(10, 11), java.util.List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11)),
      new CharRange(9, 10), java.util.List.of(
        new CharInterval(9, 9),
        new CharInterval(10, 10)),
      new CharRange(10, 12), java.util.List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11),
        new CharInterval(12, 12)),
      new CharRange(17, 25), java.util.List.of(
        new CharInterval(17, 20),
        new CharInterval(21, 25))
    )
    assert(expected == nonOverlapping)
  }

}
