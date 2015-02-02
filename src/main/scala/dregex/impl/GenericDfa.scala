package dregex.impl

import com.typesafe.scalalogging.slf4j.StrictLogging
import Util.StrictMap

case class GenericDfa[A](initial: A, transitions: Map[A, Map[NormTree.SglChar, A]], accepting: Set[A]) extends StrictLogging {

  override def toString() = s"initial: $initial; transitions: $transitions; accepting: $accepting"

  lazy val allStates =
    Set(initial) union transitions.keySet union transitions.values.map(_.values).flatten.toSet union accepting

  lazy val allButAccepting = allStates diff accepting

  lazy val allChars = transitions.values.map(_.keys).flatten.toSet
  
  /**
   * Rewrite a DFA using canonical names for the states.
   * Useful for simplifying the DFA product of intersections or NFA conversions.
   * This function does not change the language matched by the DFA
   */
  def rewrite[B](stateFactory: () => B): GenericDfa[B] = {
    val mapping = (for (state <- allStates) yield state -> stateFactory()).toMap
    GenericDfa[B](
      initial = mapping(initial),
      transitions = for ((s, fn) <- transitions) yield mapping(s) -> fn.mapValuesNow(mapping),
      accepting = accepting.map(mapping))
  }

}
