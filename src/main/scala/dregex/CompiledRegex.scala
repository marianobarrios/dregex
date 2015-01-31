package dregex

import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.LookaroundExpander
import dregex.impl.MetaDfas
import dregex.impl.MetaNormTrees
import dregex.impl.MetaNfas
import dregex.impl.RegexTree
import dregex.impl.Operations

class CompiledRegex private[dregex] (val parsedRegex: ParsedRegex, val universe: Universe) extends Regex with StrictLogging {
  
  val normTree = MetaNormTrees.normalize(parsedRegex.metaTree, universe.alphabet)
  logger.trace("norm: " + normTree)
  
  val metaNfa = MetaNfas.fromTree(normTree)
  logger.trace("meta nfa: " + metaNfa)
  
  val metaDfa = MetaDfas.fromNfa(metaNfa)
  logger.trace("meta dfa: " + metaDfa)
  
  val dfa = Operations.resolve(metaDfa)
  logger.trace("dfa: " + dfa)
  
}
