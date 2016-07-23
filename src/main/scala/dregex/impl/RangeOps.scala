package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.CharRange

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

}