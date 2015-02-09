package dregex.impl

import scala.util.parsing.combinator.JavaTokenParsers
import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.InvalidRegexException
import dregex.InvalidRegexException

class RegexParser extends JavaTokenParsers {

  override def skipWhitespace = false

  import RegexTree._

  val backslash = """\"""

  def number = """\d""".r.+ ^^ { s =>
    try {
      s.mkString.toInt
    } catch {
      case e: NumberFormatException => throw new InvalidRegexException("Cannot parse number: " + s)
    }
  }

  def charSpecialInsideClasses = backslash | "]" | "^" | "-"
  def charSpecial = backslash | "." | "|" | "(" | ")" | "[" | "]" | "+" | "*" | "?" | "^" | "$"

  def specialEscape = backslash ~ "[^dwsDWSuUxc01234567]".r ^^ {
    case _ ~ char =>
      char match {
        case "n" => Lit('\n')
        case "r" => Lit('\r')
        case "t" => Lit('\t')
        case "f" => Lit('\f')
        case "b" => Lit('\b')
        case "v" => Lit('\u000B') // vertical tab
        case "a" => Lit('\u0007') // bell
        case "e" => Lit('\u001B') // escape
        case "B" => Lit('\\')
        case c => Lit(c) // remaining escaped characters stand for themselves
      }
  }

  def hexDigit = "[0-9A-Fa-f]".r
  def octalDigit = "[0-7]".r

  def unicodeEscape = backslash ~ "u" ~ repN(4, hexDigit) ^^ {
    case _ ~ _ ~ digits =>
      Lit(Integer.parseInt(digits.mkString, 16).toChar)
  }

  def longUnicodeEscape = backslash ~ "U" ~ repN(8, hexDigit) ^^ {
    case _ ~ _ ~ digits =>
      Lit(Integer.parseInt(digits.mkString, 16).toChar)
  }

  def hexEscape = backslash ~ "x" ~ hexDigit.+ ^^ {
    case _ ~ _ ~ digits =>
      Lit(Integer.parseInt(digits.mkString, 16).toChar)
  }

  def octalEscape = backslash ~ (repN(2, octalDigit) ||| repN(3, octalDigit)) ^^ {
    case _ ~ digits =>
      Lit(Integer.parseInt(digits.mkString, 8).toChar)
  }

  def controlEscape = (backslash ~ "c" ~ ".".r) ~> failure("Unsupported feature: control escape")

  def anchor = ("^" | "$") ~> failure("Unsupported feature: anchors")

  def anyEscape = specialEscape | unicodeEscape | hexEscape | longUnicodeEscape | octalEscape | controlEscape

  def anythingExcept(parser: Parser[_]) = not(parser) ~> (".".r ^^ (x => Lit(x)))

  def charLit = anchor | anythingExcept(charSpecial) | anyEscape

  def characterClassLit = anythingExcept(charSpecialInsideClasses) | anyEscape

  def singleCharacterClassLit = characterClassLit ^^ (lit => ExtensionCharSet(lit.char))

  /*
  * For convenience, character class ranges are implemented at the parser level. This method directly
  * returns a list of all the characters included in the range 
  */
  def charClassRange = characterClassLit ~ "-" ~ characterClassLit ^^ {
    case start ~ _ ~ end => RangeCharSet(start.char, end.char)
  }

  def charClassAtom = charClassRange | singleCharacterClassLit | shorthandCharSet

  def charClass = "[" ~ "^".? ~ "-".? ~ charClassAtom.+ ~ "-".? ~ "]" ^^ {
    case _ ~ negated ~ leftDash ~ charClass ~ rightDash ~ _ =>
      val chars = if (leftDash.isDefined || rightDash.isDefined)
        charClass :+ ExtensionCharSet('-')
      else
        charClass
      negated.fold[Node](CharClass(chars: _*))(x => NegatedCharClass(chars: _*))
  }

  // There is the special case of a character class with only one character: the dash. This is valid, but
  // not easily parsed by the general constructs.
  def dashClass = "[" ~ "^".? ~ "-" ~ "]" ^^ {
    case _ ~ negated ~ _ ~ _ =>
      negated.fold[Node](CharClass(ExtensionCharSet('-')))(x => NegatedCharClass(ExtensionCharSet('-')))
  }

  val numberSet = RangeCharSet('0', '9')
  val spaceSet = ExtensionCharSet('\n', '\t', '\r', '\f', ' ')
  val wordSet = MultiRangeCharSet(numberSet, RangeCharSet('a', 'z'), RangeCharSet('A', 'Z'), ExtensionCharSet('_'))
  
  def shorthandCharSet = backslash ~ "[DWSdws]".r ^^ {
    case _ ~ "d" => numberSet
    case _ ~ "D" => CompCharSet(numberSet)
    case _ ~ "s" => spaceSet
    case _ ~ "S" => CompCharSet(spaceSet)
    case _ ~ "w" => wordSet
    case _ ~ "W" => CompCharSet(wordSet)
  }

  def shorthandCharClass = shorthandCharSet ^^ (set => CharClass(set))

  def group = "(" ~ ("?" ~ "<".? ~ "[:=!]".r).? ~ regex ~ ")" ^^ {
    case _ ~ modifiers ~ value ~ _ =>
      import Direction._
      import Condition._
      modifiers match {
        case None => value // Naked parenthesis
        case Some(_ ~ None ~ ":") => value // Non-capturing group
        case Some(_ ~ None ~ "=") => Lookaround(Ahead, Positive, value)
        case Some(_ ~ None ~ "!") => Lookaround(Ahead, Negative, value)
        case Some(_ ~ Some("<") ~ ":") => throw new InvalidRegexException("Invalid grouping: <: ")
        case Some(_ ~ Some("<") ~ "=") => Lookaround(Behind, Positive, value)
        case Some(_ ~ Some("<") ~ "!") => Lookaround(Behind, Negative, value)
        case _ => throw new AssertionError
      }
  }

  def charWildcard = "." ^^^ Wildcard

  def regexAtom =
    charLit | charWildcard | charClass | dashClass | shorthandCharClass | group

  // Lazy quantifiers (by definition) don't change whether the text matches or not, so can be ignored for our purposes

  def quantifiedBranch = regexAtom ~ ("+" | "*" | "?") ~ "?".? ^^ {
    case atom ~ "+" ~ _ => Rep(min = 1, max = -1, value = atom)
    case atom ~ "*" ~ _ => Rep(min = 0, max = -1, value = atom)
    case atom ~ "?" ~ _ => Rep(min = 0, max = 1, value = atom)
  }

  def generalQuantifier = "{" ~ number ~ ("," ~ number.?).? ~ "}" ~ "?".? ^^ {
    case _ ~ minVal ~ Some(comma ~ Some(maxVal)) ~ _ ~ _ =>
      // Quantifiers of the for {min,max}
      if (minVal <= maxVal)
        (minVal, maxVal)
      else
        throw new InvalidRegexException("invalid range in quantifier")
    case _ ~ minVal ~ Some(comma ~ None) ~ _ ~ _ =>
      // Quantifiers of the form {min,}
      (minVal, -1)
    case _ ~ minVal ~ None ~ _ ~ _ =>
      // Quantifiers of the form "{n}", the value is captured as "min", despite being also the max
      (minVal, minVal)
  }

  def generallyQuantifiedBranch = regexAtom ~ generalQuantifier ^^ {
    case atom ~ ((min, max)) => Rep(min, max, atom)
  }

  def branch = (quantifiedBranch | generallyQuantifiedBranch | regexAtom).+ ^^ {
    case Seq() => throw new AssertionError
    case Seq(first) => first
    case parts => Juxt(parts)
  }

  def emptyRegex = "" ^^^ Epsilon

  def nonEmptyRegex: Parser[Node] = branch ~ ("|" ~ regex).? ^^ {
    case left ~ Some(_ ~ right) => Disj(Seq(left, right))
    case left ~ None => left
  }

  def regex = nonEmptyRegex | emptyRegex

}

object RegexParser extends StrictLogging {

  def parse(regex: String) = {
    val parser = new RegexParser()
    parser.parseAll(parser.regex, regex) match {
      case parser.Success(ast, next) => ast
      case parser.NoSuccess((msg, next)) => throw new InvalidRegexException(msg)
    }
  }

}