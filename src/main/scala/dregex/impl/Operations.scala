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
  
  def resolve(abstractDfa: MetaDfa): Dfa = {
    val resolved = abstractDfa match {
      case DfaOperation(Operation.Intersect, left, right) => resolve(left) intersect resolve(right)
      case DfaOperation(Operation.Substract, left, right) => resolve(left) diff resolve(right)
      case atom: AtomDfa => atom.dfa.minimize()
    }
    if (resolved.impl.allStates.size >= minimizationThreshold)
      resolved.minimize()
    else 
      resolved
  }

  object Operation extends Enumeration {
    val Intersect, Substract = Value
  }

}