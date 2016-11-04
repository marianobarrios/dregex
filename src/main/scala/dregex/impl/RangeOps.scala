package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.CharRange
import scala.collection.mutable.ArrayBuffer

object RangeOps {

  def diff(left: AbstractRange, right: Seq[AbstractRange]): Seq[AbstractRange] = {
    right.foldLeft(Seq(left)) { (acc, r) =>
      diff(acc, r)
    }
  }

  private def diff(left: Seq[AbstractRange], right: AbstractRange): Seq[AbstractRange] = {
    left.map(diff(_, right)).flatten
  }

  def diff(left: AbstractRange, other: AbstractRange): Seq[AbstractRange] = {
    if (left.from >= other.from && left.to <= other.to) {
      Seq()
    } else if (left.from > other.to) {
      Seq(left)
    } else if (left.to < other.from) {
      Seq(left)
    } else if (left.from < other.from && left.to > other.to) {
      Seq(
        CharRange(from = left.from, to = other.from - 1),
        CharRange(from = other.to + 1, to = left.to))
    } else if (left.from < other.from && left.to <= other.to) {
      Seq(CharRange(from = left.from, to = other.from - 1))
    } else if (left.from >= other.from && left.to > other.to) {
      Seq(CharRange(from = other.to + 1, to = left.to))
    } else {
      throw new AssertionError
    }
  }

  /**
   * Minimize a sequence of (possibly sequential) ranges, returning a (hopefully) shorter sequence, 
   * with overlapping or contiguous ranges merged. The input sequence must be sorted.
   */
  def union(ranges: Seq[AbstractRange]): IndexedSeq[AbstractRange] = {
    /*
     * Use a mutable builder instead of a fold, because of performance issues in Scala 2.10
     */
    val builder = ArrayBuffer[AbstractRange]()
    for (range <- ranges) {
      if (builder.isEmpty) {
        builder += range
      } else {
        val newRanges = union(builder.last, range)
        builder.remove(builder.size - 1)
        builder ++= newRanges
      }
    }
    builder.toIndexedSeq
  }

  def union(left: AbstractRange, right: AbstractRange): Seq[AbstractRange] = {
    if (left.from >= right.from && left.to <= right.to) {
      Seq(right)
    } else if (left.from > right.to) {
      if (right.to + 1 == left.from)
        Seq(CharRange(right.from, left.to))
      else
        Seq(right, left)
    } else if (left.to < right.from) {
      if (left.to + 1 == right.from)
        Seq(CharRange(left.from, right.to))
      else
        Seq(left, right)
    } else if (left.from < right.from && left.to > right.to) {
      Seq(left)
    } else if (left.from < right.from && left.to <= right.to) {
      Seq(CharRange(from = left.from, to = right.to))
    } else if (left.from >= right.from && left.to > right.to) {
      Seq(CharRange(from = right.from, to = left.to))
    } else {
      throw new AssertionError
    }
  }

}