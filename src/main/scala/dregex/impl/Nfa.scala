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
      
    // Disjunction is made without any epsilon transitions, as they are not required for correctness.
    case Disj(parts) => mergeTransitions(parts.map(part => fromTreeImpl(part, from, to)): _*)

    case Rep(0, 0, value) => 
      Map(from -> Map(Epsilon -> Set(to)))
    case Rep(0, 1, value) =>
      val (int1, int2) = (new State, new State)
      mergeTransitions(
        fromTreeImpl(value, int1, int2),
        Map(from -> Map(Epsilon -> Set(int1))),
        Map(int2 -> Map(Epsilon -> Set(to))),
        Map(from -> Map(Epsilon -> Set(to))))
    case Rep(0, -1, value) =>
      val (int1, int2) = (new State, new State)
      mergeTransitions(
        fromTreeImpl(value, int1, int2),
        Map(from -> Map(Epsilon -> Set(int1))),
        Map(int2 -> Map(Epsilon -> Set(to))),
        Map(from -> Map(Epsilon -> Set(to))),
        Map(int2 -> Map(Epsilon -> Set(int1))))
    case Rep(1, 1, value) =>
      fromTreeImpl(value, from, to)

    // order is important
    case Rep(min, -1, value) if min > 0 => fromTreeImpl(Juxt(Seq(value, Rep(min - 1, -1, value))), from, to)
    case Rep(0, max, value) if max > 1 => fromTreeImpl(Juxt(Seq(Rep(0, 1, value), Rep(0, max - 1, value))), from, to)
    case Rep(min, max, value) => fromTreeImpl(Juxt(Seq(value, Rep(min - 1, max - 1, value))), from, to)

    case l: Char => Map(from -> Map(l -> Set(to)))

  }

  def mergeTransitions(transitions: Map[State, Map[NormTree.Char, Set[State]]]*): Map[State, Map[NormTree.Char, Set[State]]] = {
    transitions.reduce { (left, right) =>
      Util.merge(left, right)(Util.mergeWithUnion)
    }
  }

}