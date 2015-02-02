package dregex.impl

import scala.util.parsing.combinator.JavaTokenParsers
import com.typesafe.scalalogging.slf4j.StrictLogging

class RegexParser extends JavaTokenParsers {

  override def skipWhitespace = false

  import RegexTree._

  val backslash = """\"""
  def number = """\d""".r.+ ^^ (_.mkString.toInt)

  def charSpecialInsideClasses = backslash | "]" | "^" | "-"
  def charSpecial = backslash | "." | "|" | "(" | ")" | "[" | "]" | "+" | "*" | "?" | "^" | "$"

  def specialEscape = backslash ~ "[^dwsuUxc01234567]".r ^^ {
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

  /*
  * For convenience, character class ranges are implemented at the parser level. This method directly
  * returns a list of all the characters included in the range 
  */
  def charClassRange = characterClassLit ~ "-" ~ characterClassLit ^^ {
    case start ~ _ ~ end =>
      (start.char to end.char).map(x => Lit(x))
  }

  // In order to be compatible with the range, the literal char is also returned as a one-element list.
  def charClassAtom = (charClassRange | (characterClassLit ^^ (Seq(_))))

  def charClassContent: Parser[Seq[Lit]] = charClassAtom ~ charClassContent.? ^^ {
    case atom ~ content =>
      content match {
        case Some(c) => c ++ atom
        case None => atom
      }
  }

  def charClass = "[" ~ "^".? ~ "-".? ~ charClassContent ~ "-".? ~ "]" ^^ {
    case _ ~ negated ~ leftDash ~ charClass ~ rightDash ~ _ =>
      val chars = if (leftDash.isDefined || rightDash.isDefined)
        charClass :+ Lit('-')
      else
        charClass
      negated.fold[Node](CharClass(chars))(x => NegatedCharClass(chars))
  }

  // There is the special case of a character class with only one character: the dash. This is valid, but
  // not easily parsed by the general constructs.
  def dashClass = "[" ~ "^".? ~ "-" ~ "]" ^^ {
    case _ ~ negated ~ _ ~ _ =>
      negated.fold[Node](CharClass(Seq(Lit('-'))))(x => NegatedCharClass(Seq(Lit('-'))))
  }

  /*
   * Parse: "\d", "\w", "\s"
   * Production: "\d" -> {:type char-class :args ["0", "1", "2", ..., "9"]}
   */
  def shorthandCharClass = backslash ~ "[dws]".r ^^ {
    case _ ~ shorthand =>
      val range = shorthand match {
        case "d" => '0' to '9'
        case "w" => ('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z') ++ Seq('_')
        case "s" => Seq('\n', '\t', '\r', '\f', ' ')
      }
      CharClass(range.map(x => Lit(x)))
  }

  def group = "(" ~ ("?" ~ "<".? ~ "[:=!]".r).? ~ regex ~ ")" ^^ {
    case _ ~ modifiers ~ value ~ _ =>
      import Direction._
      import Condition._
      modifiers match {
        case None => value // Naked parenthesis
        case Some(_ ~ None ~ ":") => value // Non-capturing group
        case Some(_ ~ None ~ "=") => LookaroundExpander.simplify(Lookaround(Ahead, Positive, value))
        case Some(_ ~ None ~ "!") => LookaroundExpander.simplify(Lookaround(Ahead, Negative, value))
        case Some(_ ~ Some("<") ~ ":") => throw new Exception("Invalid grouping: <: ")
        case Some(_ ~ Some("<") ~ "=") => Lookaround(Behind, Positive, value)
        case Some(_ ~ Some("<") ~ "!") => Lookaround(Behind, Negative, value)
        case _ => throw new AssertionError
      }
  }

  def charWildcard = "." ^^^ Wildcard

  def regexAtom = charLit | charWildcard | charClass | dashClass | shorthandCharClass | group

  def quantifier = {
    import Cardinality._
    "+" ^^^ OneToInf | "*" ^^^ ZeroToInf | "?" ^^^ ZeroToOne
  }

  // Lazy quantifiers (by definition) don't change whether the text matches or not, so can be ignored for our purposes
  def quantifiedBranch = regexAtom ~ quantifier ~ "?".? ^^ { case atom ~ quant ~ _ => Quant(quant, atom) }

  def generalQuantifier = "{" ~ number ~ ("," ~ number.?).? ~ "}" ~ "?".? ^^ {
    case _ ~ minVal ~ Some(comma ~ Some(maxVal)) ~ _ ~ _ =>
      // Quantifiers of the for {min,max}
      if (minVal <= maxVal)
        (minVal, maxVal)
      else
        throw new Exception("invalid range in quantifier")
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

  def emptyRegex = "" ^^^ EmptyLit

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
      case parser.NoSuccess((msg, next)) => throw new Exception("Invalid regex: " + msg)
    }
  }

}