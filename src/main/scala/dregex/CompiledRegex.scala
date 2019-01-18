package dregex

import dregex.impl.Compiler
import dregex.impl.Dfa
import dregex.impl.SimpleState

/**
 * A fully-compiled regular expression that was generated from a string literal.
 */
class CompiledRegex private[dregex] (originalString: String, val parsedRegex: ParsedRegex, val universe: Universe)
    extends Regex {

  private[dregex] val dfa: Dfa[SimpleState] = {
    new Compiler(universe.alphabet).fromTree(parsedRegex.tree)
  }

  override def toString = s"⟪$originalString⟫ (DFA states: ${dfa.stateCount})"

}
