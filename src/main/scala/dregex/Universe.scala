package dregex

import dregex.impl.RegexTree
import dregex.impl.CharInterval
import scala.collection.immutable.Seq

/**
 * Represent the set of characters that is the union of the sets of characters of a group of regular expressions.
 * Regex must belong to the same Universe to be able to make operations between them.
 */
class Universe(parsedRegex: Seq[ParsedRegex]) {

  import RegexTree._

  val alphabet: Map[AbstractRange, Seq[CharInterval]] = {
    CharInterval.calculateNonOverlapping(parsedRegex.map(r => collect(r.tree)).flatten)
  }

  /**
   * Regular expressions can have character classes and wildcards. In order to produce a NFA, they should be expanded
   * to disjunctions. As the base alphabet is Unicode, just adding a wildcard implies a disjunction of more than one
   * million code points. Same happens with negated character classes or normal classes with large ranges.
   *
   * To prevent this, the sets are not expanded to all characters individually, but only to disjoint intervals.
   *
   * Example:
   *
   * [abc]     -> a-c
   * [^efg]    -> 0-c|h-MAX
   * mno[^efg] -> def(0-c|h-l|m|n|o|p-MAX)
   * .         -> 0-MAX
   *
   * Care must be taken when the regex is meant to be used for an operation with another regex (such as intersection
   * or difference). In this case, the sets must be disjoint across all the "universe"
   *
   * This method collects the interval, so they can then be made disjoint.
   */
  private[dregex] def collect(ast: Node): Seq[AbstractRange] = ast match {
    // Lookaround is also a ComplexPart, order important
    case Lookaround(dir, cond, value) => collect(value) :+ Wildcard
    case complex: ComplexPart => complex.values.map(collect).fold(Seq())(_ union _)
    case range: AbstractRange => Seq(range)
    case CharSet(ranges) => ranges
  }
  
}
