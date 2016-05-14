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

    case Juxt(init :+ last) =>
      // doing this iteratively prevents stack overflows in the case of long literal strings 
      var merged = Map[State, Map[NormTree.Char, Set[State]]]()
      var prev = from
      for (part <- init) {
        val int = new State
        merged = mergeTransitions(merged, fromTreeImpl(part, prev, int))
        prev = int
      }
      mergeTransitions(merged, fromTreeImpl(last, prev, to))

    case Disj(Seq()) => 
      Map()
      
    case Disj(parts) => 
      mergeTransitions(parts.map(part => fromTreeImpl(part, from, to)): _*)

    // trivial cases
      
    case Rep(1, Some(1), value) =>
      fromTreeImpl(value, from, to)
      
    case Rep(0, Some(0), value) =>
      Map(from -> Map(Epsilon -> Set(to)))

    // infinite repetitions
      
    case Rep(n, None, value) if n > 1 => 
      val juxt = Juxt(Seq.fill(n)(value) :+ Rep(0, None, value))
      fromTreeImpl(juxt, from, to)

    case Rep(1, None, value) =>
      val int1 = new State
      val int2 = new State
      mergeTransitions(
        fromTreeImpl(value, int1, int2),
        Map(from -> Map(Epsilon -> Set(int1))),
        Map(int2 -> Map(Epsilon -> Set(to))),
        Map(int2 -> Map(Epsilon -> Set(int1))))
      
    case Rep(0, None, value) =>
      val int1 = new State
      val int2 = new State
      mergeTransitions(
        fromTreeImpl(value, int1, int2),
        Map(from -> Map(Epsilon -> Set(int1))),
        Map(int2 -> Map(Epsilon -> Set(to))),
        Map(from -> Map(Epsilon -> Set(to))),
        Map(int2 -> Map(Epsilon -> Set(int1))))
      
    // finite repetitions
      
    case Rep(n, Some(m), value) if n > 1 => 
      val x = n - 1
      val juxt = Juxt(Seq.fill(x)(value) :+ Rep(1, Some(m - x), value))
      fromTreeImpl(juxt, from, to)

    case Rep(1, Some(m), value) if m > 0 =>
      // doing this iteratively prevents stack overflows in the case of long repetitions
      val int1 = new State
      var merged = fromTreeImpl(value, from, int1)
      var prev = int1
      for (i <- 1 until m - 1) {
        val int = new State
        merged = mergeTransitions(merged, fromTreeImpl(value, prev, int), Map(prev -> Map(Epsilon -> Set(to))))
        prev = int
      }
      mergeTransitions(merged, fromTreeImpl(value, prev, to), Map(prev -> Map(Epsilon -> Set(to))))
      
    case Rep(0, Some(m), value) if m > 0 =>
      // doing this iteratively prevents stack overflows in the case of long repetitions
      var merged = Map[State, Map[NormTree.Char, Set[State]]]()
      var prev = from
      for (i <- 0 until m - 1) {
        val int = new State
        merged = mergeTransitions(merged, fromTreeImpl(value, prev, int), Map(prev -> Map(Epsilon -> Set(to))))
        prev = int
      }
      mergeTransitions(merged, fromTreeImpl(value, prev, to), Map(prev -> Map(Epsilon -> Set(to))))
      

    case c: Char => 
      Map(from -> Map(c -> Set(to)))

  }

  def mergeTransitions(transitions: Map[State, Map[NormTree.Char, Set[State]]]*): Map[State, Map[NormTree.Char, Set[State]]] = {
    transitions.reduce { (left, right) =>
      Util.merge(left, right)(Util.mergeWithUnion)
    }
  }

}