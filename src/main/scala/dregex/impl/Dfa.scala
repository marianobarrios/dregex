package dregex.impl

import com.typesafe.scalalogging.StrictLogging
import dregex.impl.Util.StrictSortedMap

import scala.collection.immutable.SortedMap

case class Dfa[A <: State](
    initial: A,
    defTransitions: Map[A, SortedMap[CharInterval, A]],
    accepting: Set[A],
    minimal: Boolean = false
  ) extends StrictLogging {

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

  /**
   * Rewrite a DFA using canonical names for the states.
   * Useful for simplifying the DFA product of intersections or NFA conversions.
   * This function does not change the language matched by the DFA
   */
  def rewrite[B <: State](stateFactory: () => B): Dfa[B] = {
    val mapping = (for (state <- allStates) yield state -> stateFactory()).toMap
    Dfa[B](
      initial = mapping(initial),
      defTransitions = for ((s, fn) <- defTransitions) yield mapping(s) -> fn.mapValuesNow(mapping),
      accepting = accepting.map(mapping))
  }

}

object Dfa {

  /**
    * Match-nothing DFA
    */
  val NothingDfa = Dfa[SimpleState](initial = new SimpleState, defTransitions = Map(), accepting = Set())

}