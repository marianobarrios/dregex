package dregex.impl

import dregex.impl.Operations.Operation

object MetaTrees {
  
  trait MetaTree {
    def hasLookarounds(): Boolean
  }
  
  case class AtomTree(ast: RegexTree.Node) extends MetaTree {
    def hasLookarounds = ast.hasLookarounds()
  }
  
  case class TreeOperation(operation: Operation, left: MetaTree, right: MetaTree) extends MetaTree {
    def hasLookarounds = left.hasLookarounds || right.hasLookarounds
  }
  
}
