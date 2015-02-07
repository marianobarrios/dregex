package dregex

import dregex.impl.RegexTree
import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.LookaroundExpander
import dregex.impl.Optimizer

class ParsedRegex private[dregex] (val tree: RegexTree.Node) extends StrictLogging {

  val optimized = Optimizer.optimize(tree)
  logger.trace("optimized: " + optimized)
  
  val metaTree = LookaroundExpander.expandLookarounds(optimized)
  logger.trace("meta tree: " + metaTree)
  
}