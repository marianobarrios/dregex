package dregex.impl

import dregex.UnsupportedException
import dregex.impl.MetaTrees.AtomTree
import dregex.impl.MetaTrees.TreeOperation
import dregex.impl.MetaTrees.MetaTree
import dregex.impl.Operations.Operation
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * A meta regular expression is the intersection or substraction of 2 other (meta or simple) regular expressions.
 * Lookaround constructions are transformed in equivalent meta simple regular expressions for processing.
 * A(?=B)C is transformed into AC ∩ AB.*
 * A(?!B)C is transformed into AC - AB.*
 * A(?<=B)C is transformed into AC ∩ .*BC
 * A(?<!B)C is transformed into AC - .*BC
 *
 * In the case of more than one lookaround, the transformation is applied recursively.
 *
 * Only top level lookarounds that are part of a juxtaposition are permitted, i.e. they are no allowed inside
 * parenthesis, nested or as members of a conjunction. Additionally negative lookaheads are not allowed at the
 * end of the expression. Examples:
 *
 * Allowed:
 * A(?!B)C
 * (?!B)C
 *
 * Not allowed:
 * A(?!B)      at the end
 * (?!B)|B     part of a conjuction
 * (?!B)       unique element, not a juxtaposition
 * (?!(?!B))   lookaround inside lookaround
 * (A(?!B))B   lookaround inside parenthesis
 *
 * NOTE: Only lookahead is actually implemented, lookbehind is not.
 */
object LookaroundExpander extends StrictLogging {

  import RegexTree._
  import Direction._
  import Condition._

  /**
   * Remove trailing stars or question marks in a lookahead, which are meaningless
   */
  def simplify(lookaround: Lookaround) = lookaround match {
    case Lookaround(Ahead, cond, Juxt(init :+ a :+ b :+ Rep(0, max, _))) => Lookaround(Ahead, cond, Juxt(init :+ a :+ b)) // at least two
    case Lookaround(Ahead, cond, Juxt(Seq(first, Rep(0, max, _)))) => Lookaround(Ahead, cond, first) // one
    case Lookaround(Ahead, cond, Juxt(Seq() :+ Rep(0, max, _))) => Epsilon // nothing
    case lookbehind => lookbehind
  }

  /**
   * Optimization: combination of consecutive negative lookahead constructions
   * (?!a)(?!b)(?!c) gets combined to (?!a|b|c), which is faster to process.
   * This optimization should be applied before the lookarounds are expanded to intersections and differences.
   */
  private def combineNegLookaheads(vals: Seq[Node]) = {
    vals.foldLeft(Seq[Node]()) { (acc, x) =>
      (acc, x) match {
        case (init :+ Lookaround(Ahead, Negative, v1), Lookaround(Ahead, Negative, v2)) =>
          init :+ Lookaround(Ahead, Negative, Disj(Seq(v1, v2)))
        case _ =>
          acc :+ x
      }
    }
  }

  def expandLookarounds(tree: Node) = {
    val expanded = tree match {
      case Juxt(values) => expandImpl(combineNegLookaheads(values))
      case _ => AtomTree(tree)
    }
    // if any lookaround remains, it was non-top-level, and thas is not supported
    if (expanded.hasLookarounds)
      throw new UnsupportedException("lookaround in this position (non top-level)")
    expanded
  }

  private def expandImpl(args: Seq[Node]): MetaTree = args match {
    case first +: second +: rest => // more than one element
      first match {
        case Lookaround(Ahead, cond, value) =>
          val op = cond match {
            case Positive => Operation.Intersect
            case Negative => Operation.Substract
          }
          TreeOperation(op, expandImpl(second +: rest), AtomTree(Juxt(Seq(value, Rep(min = 0, max = -1, value = Wildcard)))))
        case Lookaround(Behind, cond, value) =>
          throw new UnsupportedException("lookbehind")
        case _ =>
          merge(first, expandImpl(second +: rest))
      }
    case first +: rest => // only one element (and also the last)
      first match {
        case Lookaround(Ahead, cond, value) => throw new UnsupportedException("lookahead in trailing position")
        case Lookaround(Behind, cond, value) => throw new UnsupportedException("lookbehind")
        case _ => AtomTree(first)
      }
  }

  private def merge(first: Node, second: MetaTree): MetaTree = (first, second) match {
    case (Juxt(firstValues), AtomTree(Juxt(secondValues))) => AtomTree(Juxt(firstValues ++ secondValues))
    case (Juxt(firstValues), AtomTree(second)) => AtomTree(Juxt(firstValues :+ second))
    case (first, AtomTree(Juxt(secondValues))) => AtomTree(Juxt(first +: secondValues))
    case (first, AtomTree(second)) => AtomTree(Juxt(Seq(first, second)))
    case (first, TreeOperation(op, left, right)) => TreeOperation(op, merge(first, left), merge(first, right))
  }

}
