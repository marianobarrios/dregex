package dregex.impl

import scala.annotation.tailrec
import scala.collection.mutable

import com.typesafe.scalalogging.StrictLogging

import Util.StrictMap
import scala.collection.immutable.SortedMap
import scala.collection.immutable.Seq

class Dfa(val impl: GenericDfa[State], val minimal: Boolean = false) extends StrictLogging {

  import State.NullState
  import Dfa.BiState
  
  override def toString() = impl.toString

  lazy val stateCount = impl.stateCount

  /*
   * Intersections, unions and differences between DFA are done using the "product construction"
   * The following pages include graphical examples of this technique:
   * http://stackoverflow.com/q/7780521/4505326
   * http://cs.stackexchange.com/a/7108 
   */

  def intersect(other: Dfa): Dfa = {
    val left = impl
    val right = other.impl
    val commonChars = left.allChars intersect right.allChars
    val newInitial = BiState(left.initial, right.initial)
    val newTransitions = for {
      (leftState, leftCharmap) <- left.defTransitions
      (rightState, rightCharmap) <- right.defTransitions
      charMap = for {
        char <- commonChars.toSeq
        leftDestState <- leftCharmap.get(char)
        rightDestState <- rightCharmap.get(char)
      } yield {
        char -> BiState(leftDestState, rightDestState)
      }
      if charMap.nonEmpty
    } yield {
      BiState(leftState, rightState) -> SortedMap(charMap: _*)
    }
    // the accepting states of the new DFA are formed by the accepting states of the intersecting DFA
    val accepting = for (l <- left.accepting; r <- right.accepting) yield BiState(l, r)
    val genericDfa = GenericDfa[BiState](newInitial, newTransitions, accepting)
    val resultDfa = Dfa.fromGenericDfa(genericDfa)
    resultDfa.removeUnreachableStates()
  }

  def diff(other: Dfa): Dfa = {
    val left = impl
    val right = other.impl
    val allChars = left.allChars union right.allChars
    val newInitial = BiState(left.initial, right.initial)
    val newTransitions = for {
      (leftState, leftCharmap) <- left.defTransitions
      rightState <- right.allStates.toSeq :+ NullState
      rightCharmap = right.transitionMap(rightState)
      charMap = for {
        char <- allChars.toSeq
        leftDestState <- leftCharmap.get(char)
        rightDestState = rightCharmap.getOrElse(char, NullState)
      } yield {
        char -> BiState(leftDestState, rightDestState)
      }
      if charMap.nonEmpty
    } yield {
      BiState(leftState, rightState) -> SortedMap(charMap: _*)
    }
    // the accepting states of the new DFA are formed by the accepting states of the left DFA, and any the states of 
    // the right DFA that are no accepting
    val accepting = for (l <- left.accepting; r <- right.allButAccepting + NullState) yield BiState(l, r)
    val genericDfa = GenericDfa[BiState](newInitial, newTransitions, accepting)
    val resultDfa = Dfa.fromGenericDfa(genericDfa)
    resultDfa.removeUnreachableStates()
  }

  def union(other: Dfa): Dfa = {
    val left = impl
    val right = other.impl
    val allChars = left.allChars union right.allChars
    val newInitial = BiState(left.initial, right.initial)
    val newTransitions = for {
      leftState <- left.allStates.toSeq :+ NullState
      leftCharmap = left.transitionMap(leftState)
      rightState <- right.allStates.toSeq :+ NullState
      rightCharmap = right.transitionMap(rightState)
      charMap = for {
        char <- allChars.toSeq
        leftDestState = leftCharmap.getOrElse(char, NullState)
        rightDestState = rightCharmap.getOrElse(char, NullState)
        if leftDestState != NullState || rightDestState != NullState
      } yield {
        char -> BiState(leftDestState, rightDestState)
      }
      if charMap.nonEmpty
    } yield {
      BiState(leftState, rightState) -> SortedMap(charMap: _*)
    }
    // the accepting states of the new DFA are formed by the accepting states of the left DFA, and any the states of 
    // the right DFA that are no accepting
    val accepting = for {
      l <- left.allStates + NullState
      r <- right.allStates + NullState
      if left.accepting.contains(l) || right.accepting.contains(r)
    } yield {
      BiState(l, r)
    }
    val genericDfa = GenericDfa[BiState](newInitial, newTransitions.toMap, accepting)
    val resultDfa = Dfa.fromGenericDfa(genericDfa)
    resultDfa.removeUnreachableStates()
  }

  /**
   * Return whether a DFA matches anything. A DFA matches at least some language if there is a path from the initial
   * state to any of the accepting states
   */
  def matchesAnything(): Boolean = {
    val visited = mutable.Set[State]()
    def hasPathToAccepting(current: State): Boolean = {
      if (impl.accepting.contains(current)) {
        true
      } else {
        visited += current
        for {
          targetState <- impl.transitionMap(current).values
          if !visited.contains(targetState)
        } {
          if (hasPathToAccepting(targetState))
            return true
        }
        false
      }
    }
    hasPathToAccepting(impl.initial)
  }

  def removeUnreachableStates(): Dfa = {
    val visited = collection.mutable.Set[State]()
    val pending = collection.mutable.Queue(impl.initial)
    while (pending.nonEmpty) {
      val currentState = pending.dequeue()
      visited += currentState
      val currentTransitions = impl.transitionMap(currentState)
      val currentPossibleTargets = currentTransitions.values.toSet
      for (targetState <- currentPossibleTargets if !visited.contains(targetState)) {
        pending.enqueue(targetState)
      }
    }
    val filteredTransitions = impl.defTransitions
      .filterKeys(visited) // using set as function
      .view.force // fix filterKeys laziness
    val filteredAccepting = impl.accepting
      .filter(visited) // using set as function
    Dfa.fromGenericDfa(
      GenericDfa(
        initial = impl.initial,
        defTransitions = filteredTransitions,
        accepting = filteredAccepting))
  }

  /**
   * DFA minimization, using Brzozowski's algorithm
   * http://cs.stackexchange.com/questions/1872/brzozowskis-algorithm-for-dfa-minimization
   */
  def minimize(): Dfa = {
    if (minimal) {
      this
    } else {
      val reversed = Dfa.fromNfa(reverse())
      Dfa.fromNfa(reversed.reverse(), minimal = true)
    }
  }

  def reverse(): Nfa = {
    val initial = new State
    val first = impl.accepting.to[Seq].map(s => NfaTransition(initial, s, Epsilon))
    val rest = for {
      (from, fn) <- impl.defTransitions
      (char, to) <- fn 
    } yield {
      NfaTransition(to, from, char)
    }
    val accepting = Set(impl.initial)
    Nfa(initial, first ++ rest.to[Seq], accepting)
  }
  
  /** 
   * Each DFA is also trivially a NFA, return it.
   */
  def toNfa(): Nfa = {
    val transitions = for {
      (state, transitionMap) <- impl.defTransitions
      (char, target) <- transitionMap
    } yield {
        NfaTransition(state, target, char)
    }
    Nfa(impl.initial, transitions.to[Seq], impl.accepting)
  }

}

object Dfa extends StrictLogging {

  case class BiState(first: State, second: State) {
    override def toString() = {
      s"$first,$second"
    }
  }
  
  /**
   * Match-nothing DFA
   */
  val NothingDfa = new Dfa(GenericDfa[State](initial = new State, defTransitions = Map(), accepting = Set()))

  case class MultiState(states: Set[State]) {
    override def toString() = {
      states.mkString(",")
    }
  }

  /**
   * Produce a DFA from a NFA using the 'power set construction'
   * https://en.wikipedia.org/w/index.php?title=Powerset_construction&oldid=547783241
   */
  def fromNfa(nfa: Nfa, minimal: Boolean = false): Dfa = {
    /*
     * Group the list of transitions of the NFA into a nested map, for easy lookup.
     * The rest of this method will use this map instead of the original list.
     */
    val transitionMap = nfa.transitions.groupBy(_.from).mapValuesNow { stateTransitions =>
      stateTransitions.groupBy(_.char).mapValuesNow { states =>
        states.map(_.to).toSet
      }
    }
    val epsilonFreeTransitions = transitionMap.mapValuesNow { trans =>
      // warn: partial function in for comprehension!
      for ((char: CharInterval, target) <- trans) yield char -> target
    }
    val epsilonExpansionCache = mutable.Map[Set[State], MultiState]()
    // Given a transition map and a set of states of a NFA, this function augments that set, following all epsilon
    // transitions recursively
    def followEpsilon(current: Set[State]) = epsilonExpansionCache.get(current).getOrElse {
      val res = followEpsilonImpl(current)
      epsilonExpansionCache(current) = res
      res
    }
    @tailrec
    def followEpsilonImpl(current: Set[State]): MultiState = {
      val immediate = for (state <- current) yield {
        transitionMap.getOrElse(state, Map()).getOrElse(Epsilon, Set())
      }
      val expanded = immediate.fold(current)(_ union _)
      if (expanded == current)
        MultiState(current)
      else
        followEpsilonImpl(expanded)
    }
    val dfaInitial = followEpsilon(Set(nfa.initial))
    val dfaTransitions = mutable.Map[MultiState, SortedMap[CharInterval, MultiState]]()
    val dfaStates = mutable.Set[MultiState]()
    val pending = mutable.Queue[MultiState](dfaInitial)
    while (pending.nonEmpty) {
      val current = pending.dequeue()
      dfaStates.add(current)
      // The set of all transition maps of the members of the current state
      val currentTrans = current.states.map(x => epsilonFreeTransitions.getOrElse(x, Map()))
      // The transition function of the current state
      val mergedCurrentTrans = currentTrans.reduceLeft(Util.mergeWithUnion)
      // use a temporary set before enqueueing to avoid adding the same state twice
      val newPending = mutable.Set[MultiState]()
      val dfaCurrentTrans = for ((char, states) <- mergedCurrentTrans) yield {
        val targetState = followEpsilon(states)
        if (!dfaStates.contains(targetState))
          newPending += targetState
        char -> targetState
      }
      pending.enqueue(newPending.toSeq: _*)
      if (dfaCurrentTrans.nonEmpty)
        dfaTransitions(current) = SortedMap(dfaCurrentTrans.toSeq: _*)
    }
    // a DFA state is accepting if any of its NFA member-states is
    val dfaAccepting = dfaStates.filter(st => Util.doIntersect(st.states, nfa.accepting)).toSet
    val genericDfa = GenericDfa(dfaInitial, dfaTransitions.toMap, dfaAccepting)
    fromGenericDfa(genericDfa, minimal)
  }

  def fromGenericDfa[A](genericDfa: GenericDfa[A], minimal: Boolean = false) =
    new Dfa(genericDfa.rewrite(() => new State), minimal)

}