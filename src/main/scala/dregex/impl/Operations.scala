package dregex.impl

import dregex.Universe
import dregex.impl.MetaTrees.MetaTree
import dregex.impl.MetaTrees.TreeOperation
import dregex.impl.MetaTrees.AtomTree

object Operations {

  /**
   * Minimization is expensive, so it is done if the number of states surpasses a a threshold. The exact number just
   * happened to work in practice.
   */
  val minimizationThreshold = 50

  def resolve(meta: MetaTree, universe: Universe): Dfa = {
    val resolved = meta match {
      case TreeOperation(op, left, right) =>
        op(resolve(left, universe), resolve(right, universe))
      case AtomTree(metaTree) =>
        val norm = Normalizer.normalize(metaTree, universe.alphabet)
        val nfa = Nfa.fromTree(norm)
        val dfa = Dfa.fromNfa(nfa)
        dfa.minimize()
    }
    if (resolved.impl.allStates.size >= minimizationThreshold)
      resolved.minimize()
    else
      resolved
  }

  type Operation = (Dfa, Dfa) => Dfa

}