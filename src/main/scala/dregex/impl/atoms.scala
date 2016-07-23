package dregex.impl

import scala.collection.JavaConversions._
import dregex.impl.RegexTree.AbstractRange
import scala.collection.mutable.ArrayBuffer

/**
 * A single or null char, i.e., including epsilon values
 */
sealed trait AtomPart

case class CharInterval(from: UnicodeChar, to: UnicodeChar) extends AtomPart with Ordered[CharInterval] {

  if (from > to)
    throw new IllegalArgumentException("from value cannot be larger than to")

  def compare(that: CharInterval): Int = this.from compare that.from

}

case object Epsilon extends AtomPart {
  override def toString = "ε"
}  

object CharInterval {
  
  def calculateNonOverlapping(ranges: Seq[AbstractRange]): Map[AbstractRange, Seq[CharInterval]] = {
    val startSet = collection.mutable.Set[UnicodeChar]()
    val endSet = collection.mutable.Set[UnicodeChar]()
    for (range <- ranges) {
      startSet.add(range.from)
      if (range.from > UnicodeChar.min) {
        endSet.add(range.from - 1)
      }
      endSet.add(range.to)
      if (range.to < UnicodeChar.max) {
        startSet.add(range.to + 1)
      }
    }
    val pairs = for (range <- ranges) yield {
      val startCopySet = new java.util.TreeSet[UnicodeChar](startSet)
      val endCopySet = new java.util.TreeSet[UnicodeChar](endSet)
      val startSubSet = startCopySet.subSet(range.from, true, range.to, true)
      val endSubSet = endCopySet.subSet(range.from, true, range.to, true)
      assert(startSubSet.size == endSubSet.size)
      val res = new ArrayBuffer[CharInterval](initialSize = startSubSet.size)
      do {
        val start = startSubSet.pollFirst()
        val end = endSubSet.pollFirst()
        res += CharInterval(from = start, to = end)
      } while (!startSubSet.isEmpty())
      range -> res.toSeq
    }
    pairs.toMap

  }

}
