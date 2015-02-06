package dregex.impl

import dregex.Universe
import dregex.impl.MetaTrees.MetaTree
import dregex.impl.MetaTrees.TreeOperation
import dregex.impl.MetaTrees.AtomTree
import com.typesafe.scalalogging.slf4j.StrictLogging

object Operations extends StrictLogging {

  /**
   * Minimization is expensive, so it is done if the number of states surpasses a a threshold. The exact number just
   * happened to work in practice.
   */
  val minimizationThreshold = 10

  def resolve(meta: MetaTree, universe: Universe): Dfa = {
    val resolved = meta match {
      case TreeOperation(op, left, right) =>
        logger.trace("op: " + meta)
        val operated = op(resolve(left, universe), resolve(right, universe))
        logger.trace("operated: " + operated)
        operated
      case AtomTree(metaTree) =>
        logger.trace("metaTree: " + metaTree)
        val norm = Normalizer.normalize(metaTree, universe.alphabet)
        logger.trace("norm: " + norm)
        val nfa = Nfa.fromTree(norm)
        logger.trace("nfa: " + nfa)
        val dfa = Dfa.fromNfa(nfa)
        logger.trace("dfa: " + nfa)
        dfa.minimize()
    }
    if (resolved.impl.allStates.size >= minimizationThreshold)
      resolved.minimize()
    else
      resolved
  }

  type Operation = (Dfa, Dfa) => Dfa

}