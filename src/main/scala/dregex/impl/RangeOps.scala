package dregex.impl

import dregex.impl.tree.AbstractRange
import dregex.impl.tree.CharRange
import scala.collection.immutable.IndexedSeq
import scala.jdk.CollectionConverters._

object RangeOps {

  def diff(left: java.util.List[AbstractRange], right: java.util.List[AbstractRange]): java.util.List[AbstractRange] = {
    left.asScala.toSeq.flatMap(diff(_, right).asScala).asJava
  }

  def diff(left: AbstractRange, right: java.util.List[AbstractRange]): java.util.List[AbstractRange] = {
    right.asScala.foldLeft(Seq(left))(diff).asJava
  }

  def diff(left: Seq[AbstractRange], right: AbstractRange): Seq[AbstractRange] = {
    left.flatMap(diff(_, right))
  }

  def diff(left: AbstractRange, right: AbstractRange): Seq[AbstractRange] = {
    if (left.from >= right.from && left.to <= right.to) {
      Seq()
    } else if (left.from > right.to) {
      Seq(left)
    } else if (left.to < right.from) {
      Seq(left)
    } else if (left.from < right.from && left.to > right.to) {
      Seq(AbstractRange.of(left.from, right.from - 1), AbstractRange.of(right.to + 1, left.to))
    } else if (left.from < right.from && left.to <= right.to) {
      Seq(AbstractRange.of(left.from, right.from - 1))
    } else if (left.from >= right.from && left.to > right.to) {
      Seq(AbstractRange.of(right.to + 1, left.to))
    } else {
      throw new AssertionError
    }
  }

  /**
    * Minimize a sequence of (possibly sequential) ranges, returning a (hopefully) shorter sequence,
    * with overlapping or contiguous ranges merged. The input sequence must be sorted.
    */
  def union(ranges: Seq[AbstractRange]): Seq[AbstractRange] = {
    ranges.foldLeft(IndexedSeq[AbstractRange]()) { (acc, range) =>
      acc match {
        case init :+ last => init ++ union(last, range)
        case Seq()        => IndexedSeq(range)
      }
    }
  }

  def union(left: AbstractRange, right: AbstractRange): Seq[AbstractRange] = {
    if (left.from >= right.from && left.to <= right.to) {
      Seq(right)
    } else if (left.from > right.to) {
      if (right.to + 1 == left.from)
        Seq(AbstractRange.of(right.from, left.to))
      else
        Seq(right, left)
    } else if (left.to < right.from) {
      if (left.to + 1 == right.from)
        Seq(AbstractRange.of(left.from, right.to))
      else
        Seq(left, right)
    } else if (left.from < right.from && left.to > right.to) {
      Seq(left)
    } else if (left.from < right.from && left.to <= right.to) {
      Seq(AbstractRange.of(left.from, right.to))
    } else if (left.from >= right.from && left.to > right.to) {
      Seq(AbstractRange.of(right.from, left.to))
    } else {
      throw new AssertionError
    }
  }

}
