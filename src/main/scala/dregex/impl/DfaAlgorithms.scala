package dregex.impl

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.collection.mutable
import Util.StrictMap

import scala.collection.immutable.Seq

object DfaAlgorithms {

  /*
   * Intersections, unions and differences between DFA are done using the "product construction"
   * The following pages include graphical examples of this technique:
   * http://stackoverflow.com/q/7780521/4505326
   * http://cs.stackexchange.com/a/7108
   */

  def intersect[A <: DfaState](left: GenericDfa[A], right: GenericDfa[A]): GenericDfa[BiState[A]] = {
    val commonChars = left.allChars intersect right.allChars
    val newInitial = BiState[A](left.initial, right.initial)
    val newTransitions = for {
      (leftState, leftCharmap) <- left.defTransitions
      (rightState, rightCharmap) <- right.defTransitions
      charMap = for {
        char <- commonChars.toSeq
        leftDestState <- leftCharmap.get(char)
        rightDestState <- rightCharmap.get(char)
      } yield {
        char -> BiState[A](leftDestState, rightDestState)
      }
      if charMap.nonEmpty
    } yield {
      BiState[A](leftState, rightState) -> SortedMap(charMap: _*)
    }
    // the accepting states of the new DFA are formed by the accepting states of the intersecting DFA
    val accepting = for (l <- left.accepting; r <- right.accepting) yield BiState(l, r)
    GenericDfa[BiState[A]](newInitial, newTransitions, accepting)
  }

  def diff[A <: DfaState](left: GenericDfa[A], right: GenericDfa[A]): GenericDfa[BiState[A]] = {
    val NullState = null.asInstanceOf[A]
    val allChars = left.allChars union right.allChars
    val newInitial = BiState[A](left.initial, right.initial)
    val newTransitions = for {
      (leftState, leftCharmap) <- left.defTransitions
      rightState <- right.allStates.toSeq :+ NullState
      rightCharmap = right.transitionMap(rightState)
      charMap = for {
        char <- allChars.toSeq
        leftDestState <- leftCharmap.get(char)
        rightDestState = rightCharmap.getOrElse(char, NullState)
      } yield {
        char -> BiState[A](leftDestState, rightDestState)
      }
      if charMap.nonEmpty
    } yield {
      BiState[A](leftState, rightState) -> SortedMap(charMap: _*)
    }
    // the accepting states of the new DFA are formed by the accepting states of the left DFA, and any the states of
    // the right DFA that are no accepting
    val accepting = for (l <- left.accepting; r <- right.allButAccepting + NullState) yield BiState[A](l, r)
    GenericDfa[BiState[A]](newInitial, newTransitions, accepting)
  }

  def union[A <: DfaState](left: GenericDfa[A], right: GenericDfa[A]): GenericDfa[BiState[A]] = {
    val NullState = null.asInstanceOf[A]
    val allChars = left.allChars union right.allChars
    val newInitial = BiState[A](left.initial, right.initial)
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
      BiState[A](leftState, rightState) -> SortedMap(charMap: _*)
    }
    // the accepting states of the new DFA are formed by the accepting states of the left DFA, and any the states of
    // the right DFA that are no accepting
    val accepting = for {
      l <- left.allStates + NullState
      r <- right.allStates + NullState
      if left.accepting.contains(l) || right.accepting.contains(r)
    } yield {
      BiState[A](l, r)
    }
    GenericDfa[BiState[A]](newInitial, newTransitions.toMap, accepting)
  }

  /**
    * Return whether a DFA matches anything. A DFA matches at least some language if there is a path from the initial
    * state to any of the accepting states
    */
  def matchesAnything[A <: DfaState](dfa: GenericDfa[A]): Boolean = {
    val visited = mutable.Set[A]()
    def hasPathToAccepting(current: A): Boolean = {
      if (dfa.accepting.contains(current)) {
        true
      } else {
        visited += current
        for {
          targetState <- dfa.transitionMap(current).values
          if !visited.contains(targetState)
        } {
          if (hasPathToAccepting(targetState))
            return true
        }
        false
      }
    }
    hasPathToAccepting(dfa.initial)
  }

  def removeUnreachableStates[A <: DfaState](dfa: GenericDfa[A]): GenericDfa[A] = {
    val visited = collection.mutable.Set[A]()
    val pending = collection.mutable.Queue(dfa.initial)
    while (pending.nonEmpty) {
      val currentState = pending.dequeue()
      visited += currentState
      val currentTransitions = dfa.transitionMap(currentState)
      val currentPossibleTargets = currentTransitions.values.toSet
      for (targetState <- currentPossibleTargets if !visited.contains(targetState)) {
        pending.enqueue(targetState)
      }
    }
    val filteredTransitions = dfa.defTransitions
      .filterKeys(visited) // using set as function
      .view.force // fix filterKeys laziness
    val filteredAccepting = dfa.accepting
      .filter(visited) // using set as function
    GenericDfa(
      initial = dfa.initial,
      defTransitions = filteredTransitions,
      accepting = filteredAccepting)
  }

  /**
    * Produce a DFA from a NFA using the 'power set construction'
    * https://en.wikipedia.org/w/index.php?title=Powerset_construction&oldid=547783241
    */
  def fromNfa(nfa: Nfa, minimal: Boolean = false): GenericDfa[MultiState] = {
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
    GenericDfa(dfaInitial, dfaTransitions.toMap, dfaAccepting, minimal)
  }

  def reverse(dfa: GenericDfa[State]): Nfa = {
    val initial = new State
    val first = dfa.accepting.to[Seq].map(s => NfaTransition(initial, s, Epsilon))
    val rest = for {
      (from, fn) <- dfa.defTransitions
      (char, to) <- fn
    } yield {
      NfaTransition(to, from, char)
    }
    val accepting = Set(dfa.initial)
    Nfa(initial, first ++ rest.to[Seq], accepting)
  }

  /**
    * Each DFA is also trivially a NFA, return it.
    */
  def toNfa(dfa: GenericDfa[State]): Nfa = {
    val transitions = for {
      (state, transitionMap) <- dfa.defTransitions
      (char, target) <- transitionMap
    } yield {
      NfaTransition(state, target, char)
    }
    Nfa(dfa.initial, transitions.to[Seq], dfa.accepting)
  }

  def rewriteWithSimpleStates[A <: DfaState](genericDfa: GenericDfa[A]): GenericDfa[State] = {
    genericDfa.rewrite(() => new State)
  }

  /**
    * DFA minimization, using Brzozowski's algorithm
    * http://cs.stackexchange.com/questions/1872/brzozowskis-algorithm-for-dfa-minimization
    */
  def minimize(dfa: GenericDfa[State]): GenericDfa[State] = {
    if (dfa.minimal) {
      dfa
    } else {
      val reversed = rewriteWithSimpleStates(fromNfa(reverse(dfa)))
      rewriteWithSimpleStates(fromNfa(reverse(reversed), minimal = true))
    }
  }

}
