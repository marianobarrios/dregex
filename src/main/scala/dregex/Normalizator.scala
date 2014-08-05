package dregex

/**
 * Regular expressions can have character classes and wildcards. In order to produce a NFA, they should be expanded
 * to disjunctions. In the case of wildcards or negated characted classes, the complete alphabet must also be known
 * to produce the expansion:
 * 
 * Example transformations with alphabet: abcdefgh
 * 
 * [abc]     -> a|b|c
 * [^abc]    -> d|e|f|g|h
 * def[^abc] -> def(d|e|f|g|h)
 * .         -> a|b|c|d|e|f|g|h
 * abc.      -> abc(a|b|c|d|e|f|g|h)
 * 
 * As the alphabet can be potentially huge (such as unicode is) something must be done to reduce the number of
 * disjunctions:
 * 
 * [abc]     -> a|b|c
 * [^abc]    -> <other_char>
 * def[^abc] -> def(d|e|f|<other_char>)
 * .         -> <other_char>
 * abc.      -> abc(a|b|c|<other_char>)
 * 
 * Where <other_char> is a special metacharacter that matches any of the characters of the alphabet not present in
 * the regex. Note that with this technique knowing the whole alphabet explicitly is not needed.
 * 
 * Care must be taken when the regex is meant to be used for an operation with another regex (such as intersection
 * or difference). In this case, <other_char> must match only the characters present in neither regex. Example:
 * 
 * Regex space: [abc] and [^cd]
 * Characters present in any regex: abcd
 * [abc] -> a|b|c
 * [^cd] -> a|b|<other_char>
 */
object Normalizator {

  def alphabet(ast: RegexPart): Set[Char] = {
    ast match {
      case Wildcard() => Set()
      case NegatedCharClass(chars) => chars.map(_.char).toSet
      case CharClass(chars) => chars.map(_.char).toSet
      case Disjunction(parts) => parts.map(alphabet _).reduce(_ union _)
      case Juxtaposition(parts) => parts.map(alphabet _).reduce(_ union _)
      case Lookaround(dir, cond, value) => alphabet(value)
      case Quantified(card, value) => alphabet(value)
      case Repetition(min, max, value) => alphabet(value)
      case Lit(char) => Set(char)
    }
  }
  
  /**
   * Expand the wildcards (\".\") and character classes, transforming them into disjunctions over the supplied alphabet
   */
  def normalize(ast: RegexPart, alphabet: Set[Char]): RegexPart = {
    ast match {
      case Wildcard() => expand(alphabet.toSeq :+ '\0')
      case NegatedCharClass(chars) => expand(alphabet.toSeq :+ '\0', chars.map(_.char))
      case CharClass(chars) => expand(chars.map(_.char))
      case Disjunction(parts) => Disjunction(parts.map(p => normalize(p, alphabet)))
      case Juxtaposition(parts) => Juxtaposition(parts.map(p => normalize(p, alphabet)))
      case Lookaround(dir, cond, value) => Lookaround(dir, cond, normalize(value, alphabet))
      case Quantified(card, value) => Quantified(card, normalize(value, alphabet))
      case Repetition(min, max, value) => Repetition(min, max, normalize(value, alphabet))
      case Lit(char) => Lit(char)
    }
  }

  private def expand(alphabet: Seq[Char], exceptions: Seq[Char] = Seq()) = 
    Disjunction((alphabet diff exceptions).map(c => Lit(c)))

}