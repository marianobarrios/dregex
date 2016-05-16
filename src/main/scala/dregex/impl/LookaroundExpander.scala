package dregex.impl

import dregex.UnsupportedException
import com.typesafe.scalalogging.slf4j.StrictLogging

/**
 * Lookaround constructions are transformed in equivalent DFA operations, and the result of those trivially transformed
 * into a NFA again for insertion into the outer expression.
 *
 * (?=B)C is transformed into C ∩ B.*
 * (?!B)C is transformed into C - B.*
 *
 * In the case of more than one lookaround, the transformation is applied recursively.
 *
 * NOTE: Only lookahead is currently implemented
 */
object LookaroundExpander extends StrictLogging {

  import RegexTree._
  import Direction._
  import Condition._
  
  def expandLookarounds(tree: Node): Node = {
    tree match {
      case juxt: Juxt => 
        expandImpl(combineNegLookaheads(juxt))
      case la: Lookaround => 
        expandLookarounds(Juxt(Seq(la)))
      case Disj(values) =>
        Disj(values.map(expandLookarounds))
      case Rep(min, max, value) =>
        Rep(min, max, expandLookarounds(value))
      case atom: AtomPart => 
        atom
      case _: Operation =>
        throw new AssertionError("operation should not exist before lookaround expansion")
    }
  }
  
  /**
   * Optimization: combination of consecutive negative lookahead constructions
   * (?!a)(?!b)(?!c) gets combined to (?!a|b|c), which is faster to process.
   * This optimization should be applied before the look-around's are expanded to intersections and differences.
   */
  private def combineNegLookaheads(juxt: Juxt): Juxt = {
    val newValues = juxt.values.foldLeft(Seq[Node]()) { (acc, x) =>
      (acc, x) match {
        case (init :+ Lookaround(Ahead, Negative, v1), Lookaround(Ahead, Negative, v2)) =>
          init :+ Lookaround(Ahead, Negative, Disj(Seq(v1, v2)))
        case _ =>
          acc :+ x
      }
    }
    Juxt(newValues)
  }
  
  private def expandImpl(juxt: Juxt): Node = {
    findLookaround(juxt.values) match {
      case Some(i) =>
        juxt.values(i).asInstanceOf[Lookaround] match {
          case Lookaround(Ahead, cond, value) =>
            val prefix = juxt.values.slice(0, i)
            val suffix = juxt.values.slice(i + 1, juxt.values.size)
            val wildcard = Rep(min = 0, max = None, value = Wildcard)
            val rightSide: Node = cond match {
              case Positive => Intersection(expandLookarounds(Juxt(suffix)), expandLookarounds(Juxt(Seq(value, wildcard))))
              case Negative => Difference(expandLookarounds(Juxt(suffix)), expandLookarounds(Juxt(Seq(value, wildcard))))
            }            
            Juxt(prefix.map(expandLookarounds) :+ rightSide)
          case Lookaround(Behind, cond, value) =>
            throw new UnsupportedException("lookbehind")
        }
      case None =>
        Juxt(juxt.values.map(expandLookarounds))
    }
  }

  private def findLookaround(args: Seq[Node]): Option[Int] = {
    val found = args.zipWithIndex.find { case (x, i) => x.isInstanceOf[Lookaround] }
    found.map { case (_, idx) => idx }
  }

}
