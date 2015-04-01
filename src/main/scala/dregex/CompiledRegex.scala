package dregex

import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.RegexTree
import dregex.impl.Operations
import dregex.impl.Util

/**
 * A fully-compiled regular expression that was generated from a string literal.
 */
class CompiledRegex private[dregex] (val parsedRegex: ParsedRegex, val universe: Universe)
  extends Regex with StrictLogging {

  val (dfa, t2) = Util.time(Operations.resolve(parsedRegex.metaTree, universe))
  logger.trace(s"Time to resolve ${t2 / 1000} ms")

}
