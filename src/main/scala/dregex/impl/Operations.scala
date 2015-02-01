package dregex.impl

import dregex.impl.MetaDfas.MetaDfa
import dregex.impl.MetaDfas.DfaOperation
import dregex.impl.MetaDfas.DfaOperation
import dregex.impl.MetaDfas.AtomDfa

object Operations {

  def resolve(abstractDfa: MetaDfa): Dfa = {
    val resolved = abstractDfa match {
      case DfaOperation(Operation.Intersect, left, right) => resolve(left) intersect resolve(right)
      case DfaOperation(Operation.Substract, left, right) => resolve(left) diff resolve(right)
      case atom: AtomDfa => atom.dfa
    }
    resolved.minimize()
  }

  object Operation extends Enumeration {
    val Intersect, Substract = Value
  }

}