package dregex

import dregex.impl.Normalizer
import dregex.impl.NormTree

class Universe(parsedRegex: Seq[ParsedRegex]) {
  val alphabet: Set[NormTree.SglChar] = 
    parsedRegex.map(r => Normalizer.alphabet(r.tree)).flatten.map(NormTree.Lit(_)).toSet + NormTree.Other
}

