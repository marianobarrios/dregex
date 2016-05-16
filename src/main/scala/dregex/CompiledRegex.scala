package dregex

import com.typesafe.scalalogging.slf4j.StrictLogging

import dregex.impl.Dfa
import dregex.impl.LookaroundExpander
import dregex.impl.Nfa
import dregex.impl.Normalizer
import dregex.impl.Compiler

/**
 * A fully-compiled regular expression that was generated from a string literal.
 */
class CompiledRegex private[dregex] (originalString: String, val parsedRegex: ParsedRegex, val universe: Universe)
    extends Regex with StrictLogging {

  val dfa: Dfa = {
    val expandedTree = LookaroundExpander.expandLookarounds(parsedRegex.tree)
    val norm = Normalizer.normalize(expandedTree, universe.alphabet)
    Compiler.fromTree(norm)
  }

  override def toString = s"[$originalString] (DFA states: ${dfa.stateCount})"

}
