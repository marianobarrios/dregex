package dregex.impl

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import scala.collection.mutable
import Util.StrictMap

import scala.collection.immutable.Seq
import scala.collection.JavaConverters.asScalaIteratorConverter
import dregex.impl.Util.StrictSortedMap

object DfaAlgorithms {

  type BinaryOp[A <: State] = (Dfa[A], Dfa[A]) => Dfa[BiState[A]]

  def union[A <: State](left: Dfa[A], right: Dfa[A]): Dfa[BiState[A]] = {
    removeUnreachableStates(
      doUnion(left, right))
  }

  def intersect[A <: State](left: Dfa[A], right: Dfa[A]): Dfa[BiState[A]] = {
    removeUnreachableStates(
      doIntersection(left, right))
  }

  def diff[A <: State](left: Dfa[A], right: Dfa[A]): Dfa[BiState[A]] = {
    removeUnreachableStates(
      doDifference(left, right))
  }

  /*
   * Intersections, unions and differences between DFA are done using the "product construction"
   * The following pages include graphical examples of this technique:
   * http://stackoverflow.com/q/7780521/4505326
   * http://cs.stackexchange.com/a/7108
   */

  private def doIntersection[A <: State](left: Dfa[A], right: Dfa[A]): Dfa[BiState[A]] = {
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
    Dfa[BiState[A]](newInitial, newTransitions, accepting)
  }

  private def doDifference[A <: State](left: Dfa[A], right: Dfa[A]): Dfa[BiState[A]] = {
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
    Dfa[BiState[A]](newInitial, newTransitions, accepting)
  }

  private def doUnion[A <: State](left: Dfa[A], right: Dfa[A]): Dfa[BiState[A]] = {
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
    Dfa[BiState[A]](newInitial, newTransitions.toMap, accepting)
  }

  /**
    * Return whether a DFA matches anything. A DFA matches at least some language if there is a path from the initial
    * state to any of the accepting states
    */
  def matchesAnything[A <: State](dfa: Dfa[A]): Boolean = {
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

  def removeUnreachableStates[A <: State](dfa: Dfa[A]): Dfa[A] = {
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
    Dfa(
      initial = dfa.initial,
      defTransitions = filteredTransitions,
      accepting = filteredAccepting)
  }

  /**
    * Produce a DFA from a NFA using the 'power set construction'
    * https://en.wikipedia.org/w/index.php?title=Powerset_construction&oldid=547783241
    */
  def fromNfa(nfa: Nfa, minimal: Boolean = false): Dfa[MultiState] = {
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
    Dfa(dfaInitial, dfaTransitions.toMap, dfaAccepting, minimal)
  }

  def reverse[A <: State](dfa: Dfa[A]): Nfa = {
    val initial: State = new SimpleState
    val first = dfa.accepting.to[Seq].map(s => NfaTransition(initial, s, Epsilon))
    val rest = for {
      (from, fn) <- dfa.defTransitions
      (char, to) <- fn
    } yield {
      NfaTransition(to, from, char)
    }
    val accepting = Set[State](dfa.initial)
    Nfa(initial, first ++ rest.to[Seq], accepting)
  }

  /**
    * Each DFA is also trivially a NFA, return it.
    */
  def toNfa[A <: State](dfa: Dfa[A]): Nfa = {
    val transitions = for {
      (state, transitionMap) <- dfa.defTransitions
      (char, target) <- transitionMap
    } yield {
      NfaTransition(state, target, char)
    }
    val accepting: Set[State] = dfa.accepting.asInstanceOf[Set[State]] // fake covariance
    Nfa(dfa.initial, transitions.to[Seq], accepting)
  }

  def rewriteWithSimpleStates[A <: State](genericDfa: Dfa[A]): Dfa[SimpleState] = {
    rewrite(genericDfa, () => new SimpleState)
  }

  /**
    * DFA minimization, using Brzozowski's algorithm
    * http://cs.stackexchange.com/questions/1872/brzozowskis-algorithm-for-dfa-minimization
    */
  def minimize(dfa: Dfa[SimpleState]): Dfa[SimpleState] = {
    if (dfa.minimal) {
      dfa
    } else {
      val reversedDfa = reverseAsDfa(dfa)
      rewriteWithSimpleStates(reverseAsDfa(reversedDfa)).copy(minimal = true)
    }
  }

  def reverseAsDfa[A <: State](dfa: Dfa[A]): Dfa[MultiState] = {
    fromNfa(reverse(dfa))
  }

  def matchString[A <: State](dfa: Dfa[A], string: String): (Boolean, Int) = {
    var current = dfa.initial
    var i = 0
    for (codePoint <- string.codePoints.iterator.asScala) {
      val char = UnicodeChar(codePoint)
      val currentTrans = dfa.defTransitions.getOrElse(current, SortedMap[CharInterval, A]())
      // O(log transitions) search in the range tree
      val newState = Util.floorEntry(currentTrans, CharInterval(from = char, to = char)).flatMap {
        case (interval, state) =>
          if (interval.to >= char) {
            Some(state)
          } else {
            None
          }
      }
      newState match {
        case Some(state) =>
          current = state
        case None =>
          return (false, i)
      }
      i += 1
    }
    (dfa.accepting.contains(current), i)
  }

  def equivalent[A <: State](left: Dfa[A], right: Dfa[A]): Boolean = {
    !matchesAnything(doDifference(left, right)) && !matchesAnything(doDifference(right, left))
  }

  def isProperSubset[A <: State](left: Dfa[A], right: Dfa[A]) = {
    !matchesAnything(doDifference(left, right)) && matchesAnything(doDifference(right, left))
  }

  def isSubsetOf[A <: State](left: Dfa[A], right: Dfa[A]) = {
    !matchesAnything(doDifference(left, right))
  }

  def isIntersectionNotEmpty[A <: State](left: Dfa[A], right: Dfa[A]) = {
    matchesAnything(doIntersection(left, right))
  }

  /**
    * Rewrite a DFA using canonical names for the states.
    * Useful for simplifying the DFA product of intersections or NFA conversions.
    * This function does not change the language matched by the DFA
    */
  def rewrite[A <: State, B <: State](dfa: Dfa[A], stateFactory: () => B): Dfa[B] = {
    val mapping = (for (state <- dfa.allStates) yield state -> stateFactory()).toMap
    Dfa[B](
      initial = mapping(dfa.initial),
      defTransitions = for ((s, fn) <- dfa.defTransitions) yield mapping(s) -> fn.mapValuesNow(mapping),
      accepting = dfa.accepting.map(mapping))
  }

}
