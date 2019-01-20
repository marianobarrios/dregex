package dregex

import dregex.impl.RegexTree

/**
 * A parsed, but uncompiled regular expression.
 */
class ParsedRegex private[dregex] (private [dregex] val tree: RegexTree.Node)