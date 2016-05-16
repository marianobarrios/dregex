package dregex.extra

import dregex.impl.Nfa
import dregex.impl.Automaton
import dregex.impl.RegexTree

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
    val transitions = for (transition <- nfa.transitions) yield {
      val weight = if (transition.char == RegexTree.Epsilon)
        1
      else
        2
      s""""${transition.from.toString}" -> "${transition.to.toString}" [label=${transition.char.toString}, weight=$weight];"""
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