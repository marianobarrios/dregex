package dregex.extra

import dregex.impl.Nfa
import dregex.impl.NormTree
import dregex.impl.Automaton

object DotFormatter {
  
  def format[A, B](nfa: Automaton[A, B]): String = {
    val states = for (state <- nfa.allStates) yield {
      val shape = if (state == nfa.initial)
        "square"
      else
        "circle"
      val peripheries = if (nfa.accepting.contains(state))
        2
      else 
        1
      s""""${state.toString}" [shape=$shape,peripheries=$peripheries];"""
    }
    val transitions = for {
      (from, localMap) <- nfa.transitions
      (char, targetSet) <- localMap
      to <- targetSet
    } yield {
      val weight = if (char == NormTree.Epsilon)
        1
      else
        2
      s""""${from.toString}" -> "${to.toString}" [label=${char.toString}, weight=$weight];"""
    }
    s"""
      digraph graphname {
        rankdir=LR;
        ${states.mkString("\n")}
        ${transitions.mkString("\n")}
      }
    """
  }
  
}