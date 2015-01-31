package dregex.impl

import dregex.impl.MetaDfas.MetaDfa
import dregex.impl.MetaDfas.DfaOperation
import dregex.impl.MetaDfas.DfaOperation
import dregex.impl.MetaDfas.AtomDfa

object Operations {

  def resolve(abstractDfa: MetaDfa): Dfa = abstractDfa match {
    case DfaOperation(Operation.Intersect, left, right) => (resolve(left) intersect resolve(right)).minimize()
    case DfaOperation(Operation.Substract, left, right) => (resolve(left) diff resolve(right)).minimize()
    case atom: AtomDfa => atom.dfa
  }

  object Operation extends Enumeration {
    val Intersect, Substract = Value
  }

}