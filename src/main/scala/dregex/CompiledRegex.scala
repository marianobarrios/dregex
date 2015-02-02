package dregex

import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.MetaDfas
import dregex.impl.MetaNormTrees
import dregex.impl.MetaNfas
import dregex.impl.RegexTree
import dregex.impl.Operations
import dregex.impl.Util

/**
 * A regular expression that was generated from a string literal.
 */
class CompiledRegex private[dregex] (val parsedRegex: ParsedRegex, val universe: Universe)
  extends Regex with StrictLogging {

  val normTree = MetaNormTrees.normalize(parsedRegex.metaTree, universe.alphabet)
  logger.trace("norm: " + normTree)

  val metaNfa = MetaNfas.fromTree(normTree)
  //logger.trace("meta nfa: " + metaNfa)

  val (metaDfa, t1) = Util.time(MetaDfas.fromNfa(metaNfa))
  //logger.trace("meta dfa: " + metaDfa)
  logger.trace(s"Time to dfa ${t1 / 1000} ms")

  val (dfa, t2) = Util.time(Operations.resolve(metaDfa))
  //logger.trace("dfa: " + dfa)
  //logger.debug("DFA states: " + dfa.impl.allStates.size)
  logger.trace(s"Time to resolve ${t2 / 1000} ms")

}
