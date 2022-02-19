package dregex

import dregex.impl.UnicodeChar
import dregex.impl.RegexTree.CharRange
import dregex.impl.RangeOps
import org.scalatest.funsuite.AnyFunSuite

class RangeOpsTest extends AnyFunSuite {

  implicit def intToUnicodeCharConversion(int: Int) = UnicodeChar(int)

  implicit def pairToRange(pair: (Int, Int)): CharRange = {
    pair match {
      case (from, to) => CharRange(UnicodeChar(from), UnicodeChar(to))
    }
  }

  test("union") {
    val ranges = Seq[CharRange]((10, 20), (9, 9), (25, 28), (3, 3), (10, 11), (9, 10), (100, 100), (101, 101))
    val union = RangeOps.union(ranges.sortBy(x => (x.from, x.to)))
    val expected: Seq[CharRange] = Seq((3, 3), (9, 20), (25, 28), (100, 101))
    assertResult(expected)(union)
  }

}
