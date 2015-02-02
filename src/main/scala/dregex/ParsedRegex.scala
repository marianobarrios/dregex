package dregex

import dregex.impl.RegexTree
import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.LookaroundExpander

class ParsedRegex private[dregex] (val tree: RegexTree.Node) extends StrictLogging {

  val metaTree = LookaroundExpander.expandLookarounds(tree)
  logger.trace("meta tree: " + metaTree)
  
}