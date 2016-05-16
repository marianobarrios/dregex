package dregex

import dregex.impl.RegexTree
import dregex.impl.AlphabetCollector

/**
 * Represent the set of characters that is the union of the sets of characters of a group of regular expressions.
 * Regex must belong to the same Universe to be able to make operations between them.
 */
class Universe(parsedRegex: Seq[ParsedRegex]) {

  val alphabet: Set[RegexTree.SglChar] = {
    val specifiedAlphabet = parsedRegex.map(r => AlphabetCollector.collect(r.tree)).flatten
    specifiedAlphabet.map(RegexTree.Lit(_)).toSet + RegexTree.Other
  }

  // TODO: toString using hash

}

