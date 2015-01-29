package dregex.impl

import dregex.impl.Operations.Operation

object MetaTrees {
  trait MetaTree
  case class AtomTree(ast: RegexTree.Node) extends MetaTree
  case class TreeOperation(operation: Operation.Value, left: MetaTree, right: MetaTree) extends MetaTree
}
