package dregex.impl

object Optimizer {

  import RegexTree._
  import Direction._
  import Condition._

  def optimize(tree: Node): Node = tree match {
    case juxt: Juxt => combineNegLookaheads(juxt)
    case cp: ComplexPart => cp.map(optimize)
    case ap: AtomPart => ap
  }
  
  /**
   * Optimization: combination of consecutive negative lookahead constructions
   * (?!a)(?!b)(?!c) gets combined to (?!a|b|c), which is faster to process.
   * This optimization should be applied before the lookarounds are expanded to intersections and differences.
   */
  def combineNegLookaheads(juxt: Juxt): Juxt = {
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

}