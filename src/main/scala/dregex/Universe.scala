package dregex

import dregex.impl.Normalizer
import dregex.impl.RegexTree

class Universe(regexAst: Seq[RegexTree.Node]) {
  val alphabet = regexAst.map(Normalizer.alphabet).reduceLeftOption(_ union _).getOrElse(Set())
}

