package dregex.impl

import com.typesafe.scalalogging.slf4j.StrictLogging
import Util.StrictSortedMap
import dregex.impl.RegexTree.AbstractRange
import scala.collection.immutable.SortedMap

case class GenericDfa[A](initial: A, defTransitions: Map[A, SortedMap[CharInterval, A]], accepting: Set[A]) 
    extends Automaton[A, CharInterval] with StrictLogging {

  case class DfaTransition(from: A, to: A, char: CharInterval) extends Transition[A, CharInterval]
  
  override def toString() = s"initial: $initial; transitions: $transitions; accepting: $accepting"

  lazy val allStates =
    Set(initial) union defTransitions.keySet union defTransitions.values.map(_.values).flatten.toSet union accepting

  lazy val allButAccepting = allStates diff accepting

  lazy val allChars = defTransitions.values.map(_.keys).flatten.toSet
  
  lazy val stateCount = allStates.size
  
  def transitionMap(state: A): SortedMap[CharInterval, A] = defTransitions.getOrElse(state, SortedMap.empty)
    
  def transitions = {
    val res = for {
      (state, transitionMap) <- defTransitions
      (char, target) <- transitionMap
    } yield {
        DfaTransition(state, target, char)
    }
    res.toSeq
  }
  
  /**
   * Rewrite a DFA using canonical names for the states.
   * Useful for simplifying the DFA product of intersections or NFA conversions.
   * This function does not change the language matched by the DFA
   */
  def rewrite[B](stateFactory: () => B): GenericDfa[B] = {
    val mapping = (for (state <- allStates) yield state -> stateFactory()).toMap
    GenericDfa[B](
      initial = mapping(initial),
      defTransitions = for ((s, fn) <- defTransitions) yield mapping(s) -> fn.mapValuesNow(mapping),
      accepting = accepting.map(mapping))
  }

}
