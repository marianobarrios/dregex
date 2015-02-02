package dregex.impl

import dregex.impl.MetaNfas.MetaNfa
import dregex.impl.MetaNfas.NfaAtom
import dregex.impl.MetaNfas.NfaOperation
import dregex.impl.Operations.Operation

object MetaDfas {

  sealed trait MetaDfa
  case class DfaOperation(operation: Operation, left: MetaDfa, right: MetaDfa) extends MetaDfa
  case class AtomDfa(dfa: Dfa) extends MetaDfa

  def fromNfa(abstractNfa: MetaNfa): MetaDfa = abstractNfa match {
    case NfaOperation(operation, left, right) => DfaOperation(operation, fromNfa(left), fromNfa(right))
    case NfaAtom(nfa) => AtomDfa(Dfa.fromNfa(nfa))
  }

}