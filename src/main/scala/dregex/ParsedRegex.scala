package dregex

import dregex.impl.{Normalization, RegexTree}

class ParsedRegex private[dregex] (val literal: String, val tree: RegexTree.Node, val norm: Normalization)