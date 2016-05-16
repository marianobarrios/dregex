package dregex.impl

import dregex.impl.RegexTree.AtomPart

/**
 * Take a regex AST and produce a NFA.
 * Except when noted the Thompson-McNaughton-Yamada algorithm is used.
 * Reference: http://stackoverflow.com/questions/11819185/steps-to-creating-an-nfa-from-a-regular-expression
 */
class Compiler(alphabet: Set[RegexTree.SglChar]) {

  import RegexTree._
  import Compiler.mergeTransitions

  /**
   * Transform a regular expression abstract syntax tree into a corresponding NFA
   */
  def fromTree(ast: Node): Dfa = {
    val initial = new State
    val accepting = new State
    val transitions = fromTreeImpl(ast, initial, accepting)
    val nfa = Nfa(initial, transitions, Set(accepting))
    Dfa.fromNfa(nfa)
  }

  private def fromTreeImpl(node: Node, from: State, to: State): Map[State, Map[AtomPart, Set[State]]] = {
    node match {
      case Wildcard =>
        fromTreeImpl(Disj(alphabet.toSeq), from, to)

      case CharClass(sets @ _*) =>
        fromTreeImpl(Disj(sets.map(_.resolve(alphabet)).flatten), from, to)

      case NegatedCharClass(sets @ _*) =>
        fromTreeImpl(Disj((alphabet diff sets.map(_.resolve(alphabet)).flatten.toSet).toSeq), from, to)

      case sglChar: SglChar =>
        Map(from -> Map(sglChar -> Set(to)))

      case Epsilon =>
        Map(from -> Map(Epsilon -> Set(to)))

      case juxt: Juxt =>
        processJuxt(juxt, from, to)
        
      case disj: Disj =>
        processDisj(disj, from, to)
        
      case rep: Rep =>
        processRep(rep, from, to)

      case Intersection(left, right) =>
        processOp(left, right, from, to, (l, r) => l intersect r)

      case Union(left, right) =>
        processOp(left, right, from, to, (l, r) => l union r)

      case Difference(left, right) =>
        processOp(left, right, from, to, (l, r) => l diff r)

      case _: Lookaround =>
        throw new IllegalStateException("lookaround should have been expanded by this time")
    }
  }

  private def processJuxt(juxt: Juxt, from: State, to: State): Map[State, Map[AtomPart, Set[State]]] = {
    juxt match {
      case Juxt(Seq()) =>
        Map(from -> Map(Epsilon -> Set(to)))

      case Juxt(Seq(head)) =>
        fromTreeImpl(head, from, to)

      case Juxt(init :+ last) =>
        // doing this iteratively prevents stack overflows in the case of long literal strings 
        var merged = Map[State, Map[AtomPart, Set[State]]]()
        var prev = from
        for (part <- init) {
          val int = new State
          merged = mergeTransitions(merged, fromTreeImpl(part, prev, int))
          prev = int
        }
        mergeTransitions(merged, fromTreeImpl(last, prev, to))
    }
  }

  private def processDisj(disj: Disj, from: State, to: State): Map[State, Map[AtomPart, Set[State]]] = {
    disj match {
      case Disj(Seq()) =>
        Map()
      case Disj(parts) =>
        mergeTransitions(parts.map(part => fromTreeImpl(part, from, to)): _*)
    }
  }

  private def processRep(rep: Rep, from: State, to: State): Map[State, Map[AtomPart, Set[State]]] = {
    rep match {

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
        var merged = Map[State, Map[AtomPart, Set[State]]]()
        var prev = from
        for (i <- 0 until m - 1) {
          val int = new State
          merged = mergeTransitions(merged, fromTreeImpl(value, prev, int), Map(prev -> Map(Epsilon -> Set(to))))
          prev = int
        }
        mergeTransitions(merged, fromTreeImpl(value, prev, to), Map(prev -> Map(Epsilon -> Set(to))))

    }
  }

  private def processOp(left: Node, right: Node, from: State, to: State, operation: (Dfa, Dfa) => Dfa) = {
    val leftDfa = fromTree(left)
    val rightDfa = fromTree(right)
    val result = operation(leftDfa, rightDfa).toNfa()
    val e: AtomPart = Epsilon // upcast
    mergeTransitions(
      Seq(
        result.transitions,
        Map(from -> Map(e -> Set(result.initial)))) ++
        result.accepting.toSeq.map(acc => Map(acc -> Map(e -> Set(to)))): _*)
  }

}

object Compiler {
  def mergeTransitions(transitions: Map[State, Map[AtomPart, Set[State]]]*): Map[State, Map[AtomPart, Set[State]]] = {
    transitions.reduce { (left, right) =>
      Util.merge(left, right)(Util.mergeWithUnion)
    }
  }
}