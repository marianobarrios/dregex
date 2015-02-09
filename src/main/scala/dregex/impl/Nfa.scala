package dregex.impl

case class Nfa(initial: State, transitions: Map[State, Map[NormTree.Char, Set[State]]], accepting: Set[State]) {
  override def toString() = {
    val transList = for ((state, charMap) <- transitions) yield {
      val map = for ((char, destination) <- charMap) yield s"$char -> ${destination.mkString("|")}"
      s"Map($state -> Map(${map.mkString(", ")}))"
    }
    val trans = transList.mkString(", ")
    s"initial: $initial; transitions: $trans; accepting: $accepting"
  }
}

/**
 * Take a regex AST and produce a NFA.
 * Except when noted the Thompson-McNaughton-Yamada algorithm is used.
 * Reference: http://stackoverflow.com/questions/11819185/steps-to-creating-an-nfa-from-a-regular-expression
 */
object Nfa {

  import NormTree._

  /**
   * Transform a regular expression abstract syntax tree into a corresponding NFA
   */
  def fromTree(ast: Node): Nfa = {
    val initial = new State
    val accepting = new State
    val transitions = fromTreeImpl(ast, initial, accepting)
    Nfa(initial, transitions, Set(accepting))
  }

  def fromTreeImpl(ast: Node, from: State, to: State): Map[State, Map[NormTree.Char, Set[State]]] = ast match {

    case Juxt(Seq()) =>
      Map(from -> Map(Epsilon -> Set(to)))

    case Juxt(Seq(head)) =>
      fromTreeImpl(head, from, to)

    // doing this iteratively prevents stack overflows in the case of long literal strings 
    case Juxt(init :+ last) =>
      var merged = Map[State, Map[NormTree.Char, Set[State]]]()
      var prev = from
      for (part <- init) {
        val int = new State
        merged = mergeTransitions(merged, fromTreeImpl(part, prev, int))
        prev = int
      }
      mergeTransitions(merged, fromTreeImpl(last, prev, to))

    case Disj(Seq()) => Map()
    case Disj(parts) => mergeTransitions(parts.map(part => fromTreeImpl(part, from, to)): _*)

    case Rep(n, -1, value) if n > 0 => fromTreeImpl(Juxt(Seq.fill(n)(value) :+ Rep(0, -1, value)), from, to)
    case Rep(n, m, value) if n > 0 => fromTreeImpl(Juxt(Seq.fill(n)(value) :+ Rep(0, m - n, value)), from, to)

    case Rep(0, -1, value) =>
      val int1 = new State
      val int2 = new State
      mergeTransitions(
        fromTreeImpl(value, int1, int2),
        Map(from -> Map(Epsilon -> Set(int1))),
        Map(int2 -> Map(Epsilon -> Set(to))),
        Map(from -> Map(Epsilon -> Set(to))),
        Map(int2 -> Map(Epsilon -> Set(int1))))

    case Rep(0, 0, value) =>
      Map(from -> Map(Epsilon -> Set(to)))
    
    // doing this iteratively prevents stack overflows in the case of long repetitions
    case Rep(0, m, value) if m > 0 =>
      var merged = Map[State, Map[NormTree.Char, Set[State]]]()
      var prev = from
      for (i <- 0 until m - 1) {
        val int = new State
        merged = mergeTransitions(merged, fromTreeImpl(value, prev, int), Map(prev -> Map(Epsilon -> Set(to))))
        prev = int
      }
      mergeTransitions(merged, fromTreeImpl(value, prev, to), Map(prev -> Map(Epsilon -> Set(to))))

    case c: Char => Map(from -> Map(c -> Set(to)))

  }

  def mergeTransitions(transitions: Map[State, Map[NormTree.Char, Set[State]]]*): Map[State, Map[NormTree.Char, Set[State]]] = {
    transitions.reduce { (left, right) =>
      Util.merge(left, right)(Util.mergeWithUnion)
    }
  }

}