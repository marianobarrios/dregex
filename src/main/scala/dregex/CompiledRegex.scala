package dregex

import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.RegexTree
import dregex.impl.Operations
import dregex.impl.Util
import dregex.impl.Dfa

/**
 * A fully-compiled regular expression that was generated from a string literal.
 */
class CompiledRegex private[dregex] (originalString: String, val parsedRegex: ParsedRegex, val universe: Universe)
  extends Regex with StrictLogging {

  val dfa: Dfa = Operations.resolve(parsedRegex.metaTree, universe)

  override def toString = s"[$originalString] (DFA states: ${dfa.stateCount})"

}
