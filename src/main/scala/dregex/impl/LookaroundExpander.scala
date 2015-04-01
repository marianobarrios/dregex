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
 * A+(?!B)C    lookaround with variable-length prefix
 *
 * NOTE: Only lookahead is currently implemented
 */
object LookaroundExpander extends StrictLogging {

  import RegexTree._
  import Direction._
  import Condition._

  def expandLookarounds(tree: Node): MetaTree = {
    val expanded = tree match {
      case la: Lookaround => 
        expandLookarounds(Juxt(Seq(la)))
      case juxt: Juxt if canDistribute(juxt) =>
        expandLookarounds(distributeLookaroundOverDisj(juxt))
      case juxt: Juxt => 
        expandImpl(juxt)
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
  
  private def canDistribute(juxt: Juxt): Boolean = {
    val idxDisj = juxt.values.indexWhere(_.isInstanceOf[Disj])
    if (idxDisj == -1)
      return false
    val idxLa = juxt.values.slice(idxDisj + 1, juxt.values.size).indexWhere(_.isInstanceOf[Lookaround])
    idxLa != -1
  }

  private def distributeLookaroundOverDisj(juxt: Juxt): Node = {
    val values = juxt.values
    val idxDisj = values.indexWhere(_.isInstanceOf[Disj])
    val idxLa = values.slice(idxDisj + 1, values.size).indexWhere(_.isInstanceOf[Lookaround])
    val disj = values(idxDisj).asInstanceOf[Disj]
    val prefix = values.slice(0, idxDisj)
    val suffix = values.slice(idxDisj + 1, values.size)
    val newDisjValues = for (v <- disj.values) yield {
      val newJuxt = Juxt((prefix :+ v) ++ suffix)
      if (canDistribute(newJuxt))
        distributeLookaroundOverDisj(newJuxt)
      else
        newJuxt
    }
    Disj(newDisjValues)
  }

  private def expandImpl(juxt: Juxt): MetaTree = {
    findLookaround(juxt.values) match {
      case Some(i) =>
        juxt.values(i).asInstanceOf[Lookaround] match {
          case Lookaround(Ahead, cond, value) =>
            val op: Operation = cond match {
              case Positive => (left, right) => left intersect right
              case Negative => (left, right) => left diff right
            }
            val prefix = juxt.values.slice(0, i)
            for (node <- prefix if node.length.isEmpty) {
              logger.trace("lookaround with variable-length prefix: " + node)
              throw new UnsupportedException("lookaround with variable-length prefix")
            }
            val suffix = juxt.values.slice(i + 1, juxt.values.size)
            val wildcard = Rep(min = 0, max = -1, value = Wildcard)
            TreeOperation(op, expandImpl(Juxt(prefix ++ suffix)), AtomTree(Juxt(prefix :+ value :+ wildcard)))
          case Lookaround(Behind, cond, value) =>
            throw new UnsupportedException("lookbehind")
        }
      case None =>
        AtomTree(juxt)
    }
  }

  private def findLookaround(args: Seq[Node]): Option[Int] = {
    val found = args.zipWithIndex.find { case (x, i) => x.isInstanceOf[Lookaround] }
    found.map { case (_, idx) => idx }
  }

}
