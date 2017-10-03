package dregex.impl

import scala.collection.immutable.Seq

case class NfaTransition(from: State, to: State, char: AtomPart)

case class Nfa(initial: State, transitions: Seq[NfaTransition], accepting: Set[State]) {

  override def toString() = {
    val transStr = transitions.mkString("[", "; ", "]")
    val acceptStr = accepting.mkString("{", "; ", "}")
    s"initial: $initial; transitions: $transStr; accepting: $acceptStr"
  }

  lazy val allStates = {
    Set(initial) ++
      transitions.map(_.from) ++
      transitions.map(_.to) ++
      accepting
  }

}
