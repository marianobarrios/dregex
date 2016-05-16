package dregex.impl

case class Nfa(initial: State, transitions: Map[State, Map[NormTree.Char, Set[State]]], accepting: Set[State])
    extends Automaton[State, NormTree.Char] {

  override def toString() = {
    val transList = for ((state, charMap) <- transitions) yield {
      val map = for ((char, destination) <- charMap) yield s"$char -> ${destination.mkString("|")}"
      s"Map($state -> Map(${map.mkString(", ")}))"
    }
    val trans = transList.mkString(", ")
    s"initial: $initial; transitions: $trans; accepting: $accepting"
  }

  lazy val allStates = {
    Set(initial) ++
      transitions.keySet ++
      transitions.values.map(_.values).toSet.flatten.flatten ++
      accepting
  }

}
