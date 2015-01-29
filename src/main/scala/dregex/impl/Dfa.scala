package dregex.impl

import scala.collection.mutable

class Dfa(val dfa: GenericDfa[State]) {

  import State.NullState

  override def toString() = dfa.toString

  case class BiState(first: State, second: State)

  /*
   * Interceptions, unions and differences between DFA are done using the "product construction"
   * The following pages include graphical examples of this technique:
   * http://stackoverflow.com/q/7780521/4505326
   * http://cs.stackexchange.com/a/7108 
   */

  /**
   * Intercept this DFA with other. The resulting DFA will only accept the strings that are accepted by both this DFA
   * and the one passed as an argument.
   */
  def intersect(other: Dfa): Dfa = product(other) { (left, right) =>
    // the accepting states of the new DFA are formed by the accepting states of the intersecting DFA
    for (l <- left.accepting; r <- right.accepting) yield BiState(l, r)
  }

  /**
   * Substract a DFA from this one. The resulting DFA will only accept the strings that are accepted by this DFA and
   * are not accepted by the one passed as an argument.
   */
  def diff(other: Dfa): Dfa = product(other) { (left, right) =>
    // the accepting states of the new DFA are formed by the accepting states of the left DFA, and any the states of 
    // the right DFA that are no accepting
    for (l <- left.accepting; r <- right.allButAccepting + NullState) yield BiState(l, r)
  }

  /**
   * Make the union between this DFA and the other. The resulting DFA will only accept the strings that are accepted 
   * either by this DFA or by the one passed as an argument.
   */
  def union(other: Dfa): Dfa = product(other) { (left, right) =>
    // the accepting states of the new DFA are formed by the accepting states of the left DFA, and any the states of 
    // the right DFA that are no accepting
    for {
      l <- left.allStates + NullState 
      r <- right.allStates + NullState
      if left.accepting.contains(l) || right.accepting.contains(r)
    } yield {
      BiState(l, r)
    }
  }

  private def product(other: Dfa)(acceptingFn: (GenericDfa[State], GenericDfa[State]) => Set[BiState]): Dfa = {
    val left = dfa
    val right = other.dfa
    val allChars = left.allChars union right.allChars
    val newInitial = BiState(left.initial, right.initial)
    val newTransitions = for {
      (leftState, leftCharmap) <- left.transitions.updated(NullState, Map())
      (rightState, rightCharmap) <- right.transitions.updated(NullState, Map())
      charMap = for {
        char <- allChars
        leftDestState = leftCharmap.getOrElse(char, NullState)
        rightDestState = rightCharmap.getOrElse(char, NullState)
        if leftDestState != NullState || rightDestState != NullState
      } yield {
        char -> BiState(leftDestState, rightDestState)
      }
      if !charMap.isEmpty
    } yield {
      BiState(leftState, rightState) -> charMap.toMap
    }
    val newDfa = GenericDfa[BiState](newInitial, newTransitions, acceptingFn(left, right))
    Dfa.fromGenericDfa(newDfa).minimize()
  }

  /**
   * Return whether a DFA matches anything. Note that the empty string is considered a match.
   * A DFA matches at least some language if there is a path from the initial state to any of the accepting states
   */
  def matchesAnything(): Boolean = {
    var visitedStates = Set[State]()
    def hasPathToAccepting(current: State): Boolean = {
      if (dfa.accepting.contains(current)) {
        true
      } else {
        visitedStates += current
        val x = for {
          targetState <- dfa.transitions.getOrElse(current, Map()).values
          if !visitedStates.contains(targetState)
        } yield {
          hasPathToAccepting(targetState)
        }
        x.find(x => x).isDefined
      }
    }
    hasPathToAccepting(dfa.initial)
  }

  /**
   * DFA minimization, using Brzozowski's algorithm
   * http://cs.stackexchange.com/questions/1872/brzozowskis-algorithm-for-dfa-minimization
   */
  def minimize(): Dfa = {
    val reversed = Dfa.fromNfa(reverse())
    Dfa.fromNfa(reversed.reverse())
  }

  def reverse(): Nfa = {
    val initial = new State
    val first = Map(initial -> Map[Nfa.Char, Set[State]](Nfa.Epsilon -> dfa.accepting))
    val rest = for ((st, fn) <- dfa.transitions; (from, to) <- fn) yield {
      Map(to -> Map[Nfa.Char, Set[State]](Nfa.LitChar(from) -> Set(st)))
    }
    val accepting = Set(dfa.initial)
    Nfa(initial, Nfa.mergeTransitions((first +: rest.toSeq): _*), accepting)
  }

}

object Dfa {

  /**
   * Match-nothing DFA
   */
  val NothingDfa = new Dfa(GenericDfa[State](initial = new State, transitions = Map(), accepting = Set()))
  
  case class MultiState(states: Set[State])

  /**
   * Produce a DFA from a NFA using the 'power set construction'
   * https://en.wikipedia.org/w/index.php?title=Powerset_construction&oldid=547783241
   */
  def fromNfa(nfa: Nfa): Dfa = {
    val epsilonFreeNfaTransitions = nfa.transitions.mapValues { trans =>
      for ((Nfa.LitChar(char), states) <- trans) yield char -> states // partial function!
    }
    var epsilonExpansions = mutable.Map[Set[State], MultiState]()
    /**
     * Given a transition map and a set of states of a NFA, this function augments that set,
     * following all epsilon transitions recursively
     */
    def followEpsilon(current: Set[State]): MultiState = {
      epsilonExpansions.get(current) match {
        case Some(state) =>
          state
        case None =>
          val res = followEpsilonImpl(current)
          epsilonExpansions(current) = res
          res
      }
    }
    def followEpsilonImpl(current: Set[State]): MultiState = {
      val transitionMaps = current.map(nfa.transitions.getOrElse(_, Map()))
      val immediate = transitionMaps.map(_.getOrElse(Nfa.Epsilon, Set()))
      val expanded = current union immediate.foldLeft(Set[State]())(_ union _)
      if (expanded == current)
        MultiState(expanded)
      else
        followEpsilon(expanded) // call memoized function!
    }
    def toDfaImpl(
      pendingStates: Seq[MultiState],
      dfaTransitions: Map[MultiState, Map[NormTree.SglChar, MultiState]] = Map()): GenericDfa[MultiState] = {
      pendingStates match {
        case current +: rest =>
          // The current DFA state (set of NFA states) augmented with the NFA states reachable by epsilon transitions
          val currentE = followEpsilon(current.states)
          // The set of all transition maps of the members of the new augmented current state
          val currentTrans = currentE.states.map(x => epsilonFreeNfaTransitions.getOrElse(x, Map()))
          // The transition function of the current state
          val dfaCurrentTrans = currentTrans.reduceLeft(Util.mergeWithUnion)
          // After the transition function is merged, augment it with the epsilon-reachable destination states
          val dfaCurrentTransE = dfaCurrentTrans.mapValues(followEpsilon)
          // Current target states can be new or already exist in the partial DFA. New ones must be enqueued for processing.
          val newPending = (dfaCurrentTransE.values.toSet diff dfaTransitions.keySet) - currentE
          toDfaImpl(rest ++ newPending, dfaTransitions.updated(currentE, dfaCurrentTransE))
        case Seq() =>
          GenericDfa[MultiState](
            initial = followEpsilon(Set(nfa.initial)),
            transitions = dfaTransitions.filterNot { case (st, map) => map.isEmpty },
            // A DFA state is accepting if any of its NFA member-states is
            accepting = dfaTransitions.keys.filter(st => Util.doIntersect(st.states, nfa.accepting)).toSet)
      }
    }
    val dfa = toDfaImpl(pendingStates = Seq(MultiState(Set(nfa.initial))))
    fromGenericDfa(dfa)
  }

  def fromGenericDfa[A](genericDfa: GenericDfa[A]) = new Dfa(genericDfa.rewrite(() => new State))

}