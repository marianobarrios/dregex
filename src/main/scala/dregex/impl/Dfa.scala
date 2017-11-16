package dregex.impl

import scala.collection.immutable.SortedMap

case class Dfa[A <: State](
    initial: A,
    defTransitions: Map[A, SortedMap[CharInterval, A]],
    accepting: Set[A],
    minimal: Boolean = false
  ) {

  override def toString() = s"initial: $initial; transitions: $defTransitions; accepting: $accepting"

  lazy val allStates = {
    Set(initial) ++
      defTransitions.keySet ++
      defTransitions.values.map(_.values).flatten.toSet ++
      accepting
  }

  lazy val allButAccepting = allStates diff accepting

  lazy val allChars = defTransitions.values.map(_.keys).flatten.toSet

  lazy val stateCount = allStates.size

  def transitionMap(state: A): SortedMap[CharInterval, A] = defTransitions.getOrElse(state, SortedMap.empty)

}

object Dfa {

  /**
    * Match-nothing DFA
    */
  val NothingDfa = Dfa[SimpleState](initial = new SimpleState, defTransitions = Map(), accepting = Set())

}