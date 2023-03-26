package dregex.extra

import dregex.impl.Epsilon
import dregex.impl.Nfa

object DotFormatter {

  def format(nfa: Nfa): String = {
    val states = for (state <- nfa.allStates) yield {
      val shape =
        if (state == nfa.initial)
          "square"
        else
          "circle"
      val peripheries =
        if (nfa.accepting.contains(state))
          2
        else
          1
      s""""${state.toString}" [shape=$shape,peripheries=$peripheries];"""
    }
    val transitions = for (transition <- nfa.transitions) yield {
      val weight =
        if (transition.char == Epsilon.instance)
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
