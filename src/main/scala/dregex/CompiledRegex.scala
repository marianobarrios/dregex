package dregex

import com.typesafe.scalalogging.StrictLogging
import dregex.impl.Compiler
import dregex.impl.Dfa
import dregex.impl.State

/**
 * A fully-compiled regular expression that was generated from a string literal.
 */
class CompiledRegex private[dregex] (originalString: String, val parsedRegex: ParsedRegex, val universe: Universe)
    extends Regex with StrictLogging {

  val dfa: Dfa[State] = {
    new Compiler(universe.alphabet).fromTree(parsedRegex.tree)
  }

  override def toString = s"⟪$originalString⟫ (DFA states: ${dfa.stateCount})"

}
