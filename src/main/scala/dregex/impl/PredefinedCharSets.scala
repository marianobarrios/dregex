package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.Lit
import dregex.impl.RegexTree.Wildcard
import dregex.impl.UnicodeChar.FromCharConversion
import dregex.impl.RegexTree.CharSet
import dregex.impl.RegexTree.CharRange

object PredefinedCharSets {

  val digit = CharSet.fromRange(CharRange(from = '0'.u, to = '9'.u))

  val space = CharSet(Seq(Lit('\n'.u), Lit('\t'.u), Lit('\r'.u), Lit('\f'.u), Lit(' '.u)))

  val wordChar =
    CharSet(digit.ranges ++
      Seq(CharRange(from = 'a'.u, to = 'z'.u),
        CharRange(from = 'A'.u, 'Z'.u),
        Lit('_'.u)))

}

