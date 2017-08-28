package dregex.impl

import scala.collection.immutable.Seq

case class NfaTransition(from: State, to: State, char: AtomPart) 

case class Nfa(initial: State, transitions: Seq[NfaTransition], accepting: Set[State]) {

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
