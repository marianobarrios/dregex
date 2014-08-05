package dregex

class State()

case class Nfa(initial: State, transitions: Map[State, Map[Char, Set[State]]], accepting: Set[State])

/**
 * Take a regex AST and produce a NFA. 
 * Except when noted the Thompson-McNaughton-Yamada algorithm is used. 
 * Reference: http://stackoverflow.com/questions/11819185/steps-to-creating-an-nfa-from-a-regular-expression
 */
object Nfa {

  /**
   * Transform a regular expression abstract syntax tree into a corresponding NFA
   */
  def toNfa(ast: RegexPart) = {
    val initial = new State()
    val accepting = new State()
    val transitions = toNfaImpl(ast, initial, accepting)
    Nfa(initial, transitions, Set(accepting))
  }

  val epsilon = Lit('\0')
  
  def toNfaImpl(ast: RegexPart, from: State, to: State): Map[State, Map[Char, Set[State]]] = {
    ast match {
      
      case Juxtaposition(head +: Seq()) => 
        toNfaImpl(head , from, to)
      case Juxtaposition(head +: rest) =>
        val int = new State
        mergeTransitions(
          toNfaImpl(head, from, int),
          toNfaImpl(Juxtaposition(rest), int, to))

      // Disjunction is made without any epsilon transitions, as they are not required for correctness.
      case Disjunction(parts) => 
        mergeTransitions(parts.map(part => toNfaImpl(part, from, to)): _*)
      
      case Lookaround(dir, cond, value) => throw new Exception("Invalid regex: lookaround in this position")
      
      case Quantified(Cardinality.OneToInf, value) => toNfaImpl(Repetition(1, -1, value), from, to)
      case Quantified(Cardinality.ZeroToInf, value) => toNfaImpl(Repetition(0, -1, value), from, to)
      case Quantified(Cardinality.ZeroToOne, value) => toNfaImpl(Repetition(0, 1, value), from, to)
      
      case Repetition(min, max, value) if min > 0 => 
        toNfaImpl(Juxtaposition(Seq(value, Repetition(min - 1, max, value))), from, to)
      case Repetition(0, max, value) if max > 0 => 
        toNfaImpl(Juxtaposition(Seq(Repetition(0, 1, value), Repetition(0, max - 1, value))), from, to)
      case Repetition(0, 1, value) =>
        val (int1, int2) = (new State, new State)
        mergeTransitions(
          toNfaImpl(value, int1, int2),
          toNfaImpl(epsilon, from, int1),
          toNfaImpl(epsilon, int2, to),
          toNfaImpl(epsilon, from, to))
      case Repetition(0, -1, value) =>
        val (int1, int2) = (new State, new State)
        mergeTransitions(
          toNfaImpl(value, int1, int2),
          toNfaImpl(epsilon, from, int1),
          toNfaImpl(epsilon, int2, to),
          toNfaImpl(epsilon, from, to),
          toNfaImpl(epsilon, int2, int1))
          
      case Lit(char) =>
        Map(from -> Map(char -> Set(to)))
        
    }
  }

  private def mergeTransitions(transitions: Map[State, Map[Char, Set[State]]]*) = {
    transitions.reduce { (left, right) =>
      Util.mergeNestedWithUnion(left, right)
    }
  }

}