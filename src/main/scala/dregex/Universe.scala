package dregex

import dregex.impl.Normalizer
import dregex.impl.NormTree

/**
 * Represent the set of characters that is the union of the sets of characters of a group of regular expressions. 
 * Regex must belong to the same Universe to be able to make operations between them.
 */
class Universe(parsedRegex: Seq[ParsedRegex]) {
  val alphabet: Set[NormTree.SglChar] = 
    parsedRegex.map(r => Normalizer.alphabet(r.tree)).flatten.map(NormTree.Lit(_)).toSet + NormTree.Other
}

