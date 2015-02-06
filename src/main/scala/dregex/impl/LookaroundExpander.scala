package dregex.impl

import dregex.UnsupportedException
import dregex.impl.MetaTrees.AtomTree
import dregex.impl.MetaTrees.TreeOperation
import dregex.impl.MetaTrees.MetaTree
import dregex.impl.Operations.Operation
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * A meta regular expression is the intersection or subtraction of 2 other (meta or simple) regular expressions.
 * Lookaround constructions are transformed in equivalent meta simple regular expressions for processing.
 *
 * A(?=B)C is transformed into AC ∩ AB.*
 * A(?!B)C is transformed into AC - AB.*
 *
 * In the case of more than one lookaround, the transformation is applied recursively.
 *
 * This works if A is of known length
 *
 * Only top level lookarounds that are part of a juxtaposition are permitted, i.e. they are no allowed inside
 * parenthesis, nested or as members of a conjunction. Examples:
 *
 * Allowed:
 * A(?!B)C
 * (?!B)C
 *
 * Not allowed:
 * (?!B)|B     part of a conjuction
 * (?!(?!B))   lookaround inside lookaround
 * (A(?!B))B   lookaround inside parenthesis
 *
 * NOTE: Only lookahead is currently implemented
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

  // TODO: Consider just a LA
  def expandLookarounds(tree: Node): MetaTree = {
    val expanded = tree match {
      case Juxt(values) =>
        expandImpl(combineNegLookaheads(values))
      case Disj(values) if values.exists(_.hasLookarounds) =>
        val first +: second +: rest = values
        val op: Operation = (left, right) => left union right
        val firstOp = TreeOperation(op, expandLookarounds(first), expandLookarounds(second))
        rest.foldLeft(firstOp) { (acc, value) =>
          TreeOperation(op, acc, expandLookarounds(value))
        }
      case _ =>
        AtomTree(tree)
    }
    // if any lookaround remains, it was non-top-level, and that is not supported
    if (expanded.hasLookarounds)
      throw new UnsupportedException("lookaround in this position")
    expanded
  }

  private def expandImpl(args: Seq[Node]): MetaTree = {
    findLookaround(args) match {
      case Some(i) =>
        args(i).asInstanceOf[Lookaround] match {
          case Lookaround(Ahead, cond, value) =>
            val op: Operation = cond match {
              case Positive => (left, right) => left intersect right
              case Negative => (left, right) => left diff right
            }
            val prefix = args.slice(0, i)
            for (node <- prefix if node.length.isEmpty)
              throw new UnsupportedException("lookaround with variable-length prefix")
            val suffix = args.slice(i + 1, args.size)
            val wildcard = Rep(min = 0, max = -1, value = Wildcard)
            TreeOperation(op, expandImpl(prefix ++ suffix), AtomTree(Juxt(prefix :+ value :+ wildcard)))
          case Lookaround(Behind, cond, value) =>
            throw new UnsupportedException("lookbehind")
        }
      case None =>
        AtomTree(Juxt(args))
    }
  }

  private def findLookaround(args: Seq[Node]): Option[Int] = {
    val found = args.zipWithIndex.find { case (x, i) => x.isInstanceOf[Lookaround] }
    found.map { case (_, idx) => idx }
  }

}
