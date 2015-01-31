package dregex

import dregex.impl.Normalizer

class Universe(parsedRegex: Seq[ParsedRegex]) {
  val alphabet = parsedRegex.map(r => Normalizer.alphabet(r.tree)).fold(Set())(_ union _)
}

