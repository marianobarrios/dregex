package dregex

import dregex.impl.Normalizer
import dregex.impl.tree.Node

class ParsedRegex private[dregex] (val literal: String, val tree: Node, val norm: Normalizer)