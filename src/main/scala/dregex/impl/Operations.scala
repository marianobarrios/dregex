package dregex.impl

import dregex.impl.MetaDfas.MetaDfa
import dregex.impl.MetaDfas.DfaOperation
import dregex.impl.MetaDfas.AtomDfa

object Operations {

  /**
   * Minimization is expensive, so it is done if the number of states surpasses a a threshold. The exact number just 
   * happened to work in practice.
   */
  val minimizationThreshold = 50
  
  def resolve(meta: MetaDfa): Dfa = {
    val resolved = meta match {
      case DfaOperation(op, left, right) => op(resolve(left), resolve(right))
      case AtomDfa(dfa) => dfa.minimize()
    }
    if (resolved.impl.allStates.size >= minimizationThreshold)
      resolved.minimize()
    else 
      resolved
  }

  type Operation = (Dfa, Dfa) => Dfa
  
}