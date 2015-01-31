package dregex

import dregex.impl.Dfa

class SynteticRegex private[dregex] (val dfa: Dfa, val universe: Universe) extends Regex

