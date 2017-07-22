package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.CharRange
import scala.collection.immutable.Seq
import scala.collection.immutable.IndexedSeq

object RangeOps {

  def diff(left: AbstractRange, right: Seq[AbstractRange]): Seq[AbstractRange] = {
    right.foldLeft(Seq(left))(diff _)
    //right.map(diff(left, _)).flatten
  }

  def diff(left: Seq[AbstractRange], right: AbstractRange): Seq[AbstractRange] = {
    left.map(diff(_, right)).flatten
  }

  def diff(left: AbstractRange, right: AbstractRange): Seq[AbstractRange] = {
    if (left.from >= right.from && left.to <= right.to) {
      Seq()
    } else if (left.from > right.to) {
      Seq(left)
    } else if (left.to < right.from) {
      Seq(left)
    } else if (left.from < right.from && left.to > right.to) {
      Seq(
        CharRange(from = left.from, to = right.from - 1),
        CharRange(from = right.to + 1, to = left.to))
    } else if (left.from < right.from && left.to <= right.to) {
      Seq(CharRange(from = left.from, to = right.from - 1))
    } else if (left.from >= right.from && left.to > right.to) {
      Seq(CharRange(from = right.to + 1, to = left.to))
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
        case Seq() => IndexedSeq(range)
      }
    }
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