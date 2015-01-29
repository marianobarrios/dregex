package dregex.impl

import dregex.impl.MetaTrees.MetaTree
import dregex.impl.MetaTrees.TreeOperation
import dregex.impl.MetaTrees.AtomTree
import dregex.impl.Operations.Operation

object MetaNormTrees {
  
  trait MetaNormTree
  case class NormTreeAtom(ast: NormTree.Node) extends MetaNormTree
  case class NormTreeOperation(operation: Operation.Value, left: MetaNormTree, right: MetaNormTree) extends MetaNormTree

  def normalize(ast: MetaTree, alphabet: Set[Char]): MetaNormTree = ast match {
    case TreeOperation(op, left, right) => NormTreeOperation(op, normalize(left, alphabet), normalize(right, alphabet))
    case AtomTree(ast) => NormTreeAtom(Normalizer.normalize(ast, alphabet))
  }

}