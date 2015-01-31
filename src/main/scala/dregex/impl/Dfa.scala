package dregex.impl

import scala.collection.mutable
import scala.annotation.tailrec
import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.NormTree.SglChar
import Util.StrictMap

class Dfa(val dfa: GenericDfa[State]) extends StrictLogging {

  import State.NullState

  override def toString() = dfa.toString

  case class BiState(first: State, second: State)

  /*
   * Intersections, unions and differences between DFA are done using the "product construction"
   * The following pages include graphical examples of this technique:
   * http://stackoverflow.com/q/7780521/4505326
   * http://cs.stackexchange.com/a/7108 
   */

  /**
   * Intersect this DFA with other. The resulting DFA will only accept the strings that are accepted by both this DFA
   * and the one passed as an argument.
   */
  def intersect(other: Dfa): Dfa = {
    val left = dfa
    val right = other.dfa
    val commonChars = left.allChars intersect right.allChars
    val newInitial = BiState(left.initial, right.initial)
    val newTransitions = for {
      (leftState, leftCharmap) <- left.transitions.updated(NullState, Map())
      (rightState, rightCharmap) <- right.transitions.updated(NullState, Map())
      charMap = for {
        char <- commonChars
        leftDestState <- leftCharmap.get(char)
        rightDestState <- rightCharmap.get(char)
      } yield {
        char -> BiState(leftDestState, rightDestState)
      }
      if !charMap.isEmpty
    } yield {
      BiState(leftState, rightState) -> charMap.toMap
    }
    // the accepting states of the new DFA are formed by the accepting states of the intersecting DFA
    val accepting = for (l <- left.accepting; r <- right.accepting) yield BiState(l, r)
    val newDfa = GenericDfa[BiState](newInitial, newTransitions, accepting)
    Dfa.fromGenericDfa(newDfa)
  }

  /**
   * Substract a DFA from this one. The resulting DFA will only accept the strings that are accepted by this DFA and
   * are not accepted by the one passed as an argument.
   */
  def diff(other: Dfa): Dfa = {
    val left = dfa
    val right = other.dfa
    val allChars = left.allChars union right.allChars
    val newInitial = BiState(left.initial, right.initial)
    val newTransitions = for {
      (leftState, leftCharmap) <- left.transitions.updated(NullState, Map())
      (rightState, rightCharmap) <- right.transitions.updated(NullState, Map())
      charMap = for {
        char <- allChars
        leftDestState <- leftCharmap.get(char)
        rightDestState = rightCharmap.getOrElse(char, NullState)
      } yield {
        char -> BiState(leftDestState, rightDestState)
      }
      if !charMap.isEmpty
    } yield {
      BiState(leftState, rightState) -> charMap.toMap
    }
    // the accepting states of the new DFA are formed by the accepting states of the left DFA, and any the states of 
    // the right DFA that are no accepting
    val accepting = for (l <- left.accepting; r <- right.allButAccepting + NullState) yield BiState(l, r)
    val newDfa = GenericDfa[BiState](newInitial, newTransitions, accepting)
    Dfa.fromGenericDfa(newDfa)
  }

  /**
   * Make the union between this DFA and the other. The resulting DFA will only accept the strings that are accepted
   * either by this DFA or by the one passed as an argument.
   */
  def union(other: Dfa): Dfa = {
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
    // the accepting states of the new DFA are formed by the accepting states of the left DFA, and any the states of 
    // the right DFA that are no accepting
    val accepting = for {
      l <- left.allStates + NullState
      r <- right.allStates + NullState
      if left.accepting.contains(l) || right.accepting.contains(r)
    } yield {
      BiState(l, r)
    }
    val newDfa = GenericDfa[BiState](newInitial, newTransitions, accepting)
    Dfa.fromGenericDfa(newDfa)
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

object Dfa extends StrictLogging {

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
    val epsilonFreeTransitions = nfa.transitions.mapValuesNow { trans =>
     for ((Nfa.LitChar(char), target) <- trans) yield char -> target // partial function!
    }
    val epsilonExpansionCache = mutable.Map[Set[State], MultiState]()
    /**
     * Given a transition map and a set of states of a NFA, this function augments that set, following all epsilon
     * transitions recursively
     */
    def followEpsilon(current: Set[State]) = epsilonExpansionCache.get(current).getOrElse {
      val res = followEpsilonImpl(current)
      epsilonExpansionCache(current) = res
      res
    }
    @tailrec
    def followEpsilonImpl(current: Set[State]): MultiState = {
      val immediate = for (state <- current) yield {
        nfa.transitions.getOrElse(state, Map()).getOrElse(Nfa.Epsilon, Set())
      }
      val expanded = immediate.fold(current)(_ union _)
      if (expanded == current)
        MultiState(current)
      else
        followEpsilonImpl(expanded)
    }
    val dfaInitial = followEpsilon(Set(nfa.initial))
    val dfaTransitions = mutable.Map[MultiState, Map[SglChar, MultiState]]()
    val dfaStates = mutable.Set[MultiState]()
    val pending = mutable.Queue[MultiState](dfaInitial)
    while (!pending.isEmpty) {
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
      if (!dfaCurrentTrans.isEmpty)
        dfaTransitions(current) = dfaCurrentTrans
    }
    // a DFA state is accepting if any of its NFA member-states is
    val dfaAccepting = dfaStates.filter(st => Util.doIntersect(st.states, nfa.accepting)).toSet
    fromGenericDfa(GenericDfa(dfaInitial, dfaTransitions.toMap, dfaAccepting))
  }

  def fromGenericDfa[A](genericDfa: GenericDfa[A]) = new Dfa(genericDfa.rewrite(() => new State))

}