package dregex.impl

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
object AlphabetCollector {

  import RegexTree._
  
  def collect(ast: Node): Set[UnicodeChar] = ast match {
    case complex: ComplexPart => complex.values.map(collect).reduce(_ union _)
    case Lit(char) => Set(char)
    case Epsilon => Set()
    case Wildcard => Set()
    case CharClass(sets @ _*) => sets.map(_.chars).flatten.toSet
    case NegatedCharClass(sets @ _*) => sets.map(_.chars).flatten.toSet
    case Other => throw new IllegalArgumentException("found 'other' atom before normalization")
  }
  
}
