package dregex.impl

case class NfaTransition(from: State, to: State, char: AtomPart) 
    extends Transition[State, AtomPart]

case class Nfa(initial: State, transitions: Seq[NfaTransition], accepting: Set[State])
    extends Automaton[State, AtomPart] {

  override def toString() = {
    s"initial: $initial; transitions: $transitions; accepting: $accepting"
  }

  lazy val allStates = {
    Set(initial) ++
      transitions.map(_.from) ++
      transitions.map(_.to) ++
      accepting
  }

}
