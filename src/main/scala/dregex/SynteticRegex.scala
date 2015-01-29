package dregex

import dregex.impl.Dfa

class SynteticRegex(val dfa: Dfa, val universe: Universe) extends Regex

