package dregex

import dregex.impl.RangeOps
import dregex.impl.tree.CharRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import scala.jdk.CollectionConverters._

class RangeOpsTest {

  implicit def pairToRange(pair: (Int, Int)): CharRange = {
    pair match {
      case (from, to) => new CharRange(from, to)
    }
  }

  @Test
  def testUnion() = {
    val ranges = Seq[CharRange]((10, 20), (9, 9), (25, 28), (3, 3), (10, 11), (9, 10), (100, 100), (101, 101))
    val union = RangeOps.union(ranges.sortBy(x => (x.from, x.to)).asJava)
    val expected = Seq[CharRange]((3, 3), (9, 20), (25, 28), (100, 101)).asJava
    assertEquals(expected, union)
  }

}
