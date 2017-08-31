package dregex.impl

import com.typesafe.scalalogging.StrictLogging

class Dfa(val impl: GenericDfa[State]) extends StrictLogging {

  type BinarySetOperation[A <: DfaState] = (GenericDfa[A], GenericDfa[A]) => GenericDfa[BiState[A]]

  override def toString() = impl.toString
  def stateCount = impl.stateCount

  private def doSetOperation(other: Dfa, op: BinarySetOperation[State]) = {
    new Dfa(
      DfaAlgorithms.rewriteWithSimpleStates(
        DfaAlgorithms.removeUnreachableStates(
          op(this.impl, other.impl))))
  }

  def intersect(other: Dfa): Dfa = {
    doSetOperation(other, DfaAlgorithms.intersect)
  }

  def diff(other: Dfa): Dfa = {
    doSetOperation(other, DfaAlgorithms.diff)
  }

  def union(other: Dfa): Dfa = {
    doSetOperation(other, DfaAlgorithms.union)
  }

  def matchesAnything(): Boolean = {
    DfaAlgorithms.matchesAnything(impl)
  }

  def minimize(): Dfa = {
    new Dfa(DfaAlgorithms.minimize(impl))
  }

}

object Dfa extends StrictLogging {

  /**
   * Match-nothing DFA
   */
  val NothingDfa = new Dfa(GenericDfa[State](initial = new State, defTransitions = Map(), accepting = Set()))

  def fromNfa(nfa: Nfa, minimal: Boolean = false): Dfa = {
    new Dfa(
      DfaAlgorithms.rewriteWithSimpleStates(
        DfaAlgorithms.fromNfa(nfa)))
  }

}