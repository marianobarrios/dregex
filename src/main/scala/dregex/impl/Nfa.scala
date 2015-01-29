package dregex.impl

case class Nfa(initial: State, transitions: Map[State, Map[Nfa.Char, Set[State]]], accepting: Set[State]) {
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

  sealed trait Char

  case class LitChar(char: NormTree.SglChar) extends Char {
    override def toString() = char.toString()
  }

  case object Epsilon extends Char {
    override def toString() = "ε"
  }

  /**
   * Transform a regular expression abstract syntax tree into a corresponding NFA
   */
  def fromTree(ast: Node): Nfa = {
    val initial = new State()
    val accepting = new State()
    val transitions = fromTreeImpl(ast, initial, accepting)
    Nfa(initial, transitions, Set(accepting))
  }

  def fromTreeImpl(ast: Node, from: State, to: State): Map[State, Map[Nfa.Char, Set[State]]] = ast match {

    case Juxt(Seq(head)) =>
      fromTreeImpl(head, from, to)
    case Juxt(head +: rest) =>
      val int = new State
      mergeTransitions(
        fromTreeImpl(head, from, int),
        fromTreeImpl(Juxt(rest), int, to))

    // Disjunction is made without any epsilon transitions, as they are not required for correctness.
    case Disj(parts) => mergeTransitions(parts.map(part => fromTreeImpl(part, from, to)): _*)

    case Quant(Cardinality.OneToInf, value) => fromTreeImpl(Rep(1, -1, value), from, to)
    case Quant(Cardinality.ZeroToInf, value) => fromTreeImpl(Rep(0, -1, value), from, to)
    case Quant(Cardinality.ZeroToOne, value) => fromTreeImpl(Rep(0, 1, value), from, to)

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

    case l: SglChar => Map(from -> Map(LitChar(l) -> Set(to)))
    case EmptyLit() => Map(from -> Map(Epsilon -> Set(to)))

  }

  def mergeTransitions(transitions: Map[State, Map[Nfa.Char, Set[State]]]*) = {
    transitions.reduce { (left, right) =>
      Util.mergeNestedWithUnion(left, right)
    }
  }

}