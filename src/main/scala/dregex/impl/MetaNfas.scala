package dregex.impl

import dregex.impl.MetaNormTrees.MetaNormTree
import dregex.impl.MetaNormTrees.NormTreeOperation
import dregex.impl.MetaNormTrees.NormTreeAtom
import dregex.impl.Operations.Operation

object MetaNfas {

  sealed trait MetaNfa
  case class NfaOperation(operation: Operation, left: MetaNfa, right: MetaNfa) extends MetaNfa
  case class NfaAtom(nfa: Nfa) extends MetaNfa

  def fromTree(abstractAst: MetaNormTree): MetaNfa = abstractAst match {
    case NormTreeOperation(operation, left, right) => NfaOperation(operation, fromTree(left), fromTree(right))
    case NormTreeAtom(nfa) => NfaAtom(Nfa.fromTree(nfa))
  }

}