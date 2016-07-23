package dregex

import org.scalatest.FunSuite
import dregex.impl.UnicodeChar
import org.scalatest.Matchers
import dregex.impl.RegexTree
import dregex.impl.RegexTree.AbstractRange
import dregex.impl.CharInterval
import dregex.impl.RegexTree.CharRange

class CharIntervalTest extends FunSuite with Matchers {

  implicit def intToUnicodeCharConversion(int: Int) = UnicodeChar(int)

  implicit def pairToRange(pair: (Int, Int)): CharRange = {
    pair match {
      case (from, to) => CharRange(UnicodeChar(from), UnicodeChar(to))
    }
  }

  implicit def pairToInterval(pair: (Int, Int)): CharInterval = {
    pair match {
      case (from, to) => CharInterval(UnicodeChar(from), UnicodeChar(to))
    }
  }

  test("non-overlapping") {
    val ranges = Seq[CharRange]((10, 20), (21, 30), (0, 100), (9, 9), (10, 11), (9, 10), (10, 12), (17, 25))
    val nonOverlapping = CharInterval.calculateNonOverlapping(ranges)
    val expected = Map[CharRange, Seq[CharInterval]](
      CharRange(10, 20) -> Seq((10, 10), (11, 11), (12, 12), (13, 16), (17, 20)),
      CharRange(21, 30) -> Seq((21, 25), (26, 30)),
      CharRange(0, 100) -> Seq((0, 8), (9, 9), (10, 10), (11, 11), (12, 12), (13, 16), (17, 20), (21, 25), (26, 30), (31, 100)),
      CharRange(9, 9) -> Seq((9, 9)),
      CharRange(10, 11) -> Seq((10, 10), (11, 11)),
      CharRange(9, 10) -> Seq((9, 9), (10, 10)),
      CharRange(10, 12) -> Seq((10, 10), (11, 11), (12, 12)),
      CharRange(17, 25) -> Seq((17, 20), (21, 25)))
    assertResult(expected)(nonOverlapping)
  }

}