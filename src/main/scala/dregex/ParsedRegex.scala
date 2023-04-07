package dregex

import dregex.impl.{Normalizer, RegexTree}

class ParsedRegex private[dregex] (val literal: String, val tree: RegexTree.Node, val norm: Normalizer)