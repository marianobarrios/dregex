package dregex.impl

import scala.util.parsing.combinator.JavaTokenParsers
import dregex.InvalidRegexException
import dregex.impl.UnicodeChar.FromCharConversion
import scala.collection.immutable.Seq

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

  def specialEscape = backslash ~ "[^dwsDWSuxcpR0123456789]".r ^^ {
    case _ ~ char =>
      char match {
        case "n" => Lit('\n'.u)
        case "r" => Lit('\r'.u)
        case "t" => Lit('\t'.u)
        case "f" => Lit('\f'.u)
        case "b" => Lit('\b'.u)
        case "v" => Lit('\u000B'.u) // vertical tab
        case "a" => Lit('\u0007'.u) // bell
        case "e" => Lit('\u001B'.u) // escape
        case "B" => Lit('\\'.u)
        case c => Lit(UnicodeChar.fromSingletonString(c)) // remaining escaped characters stand for themselves
      }
  }

  def hexDigit = """\p{XDigit}""".r
  def octalDigit = "[0-7]".r

  def doubleUnicodeEscape = backslash ~ "u" ~ repN(4, hexDigit) ~ backslash ~ "u" ~ repN(4, hexDigit) ^? {
    case _ ~ _ ~ highDigits ~ _ ~ _ ~ lowDigits if isHighSurrogate(highDigits) && isLowSurrogate(lowDigits) =>
      val high = Integer.parseInt(highDigits.mkString, 16).toChar
      val low = Integer.parseInt(lowDigits.mkString, 16).toChar
      val codePoint = Character.toCodePoint(high, low)
      Lit(UnicodeChar(codePoint))
  }

  private def isHighSurrogate(digits: List[String]) = {
    Character.isHighSurrogate(Integer.parseInt(digits.mkString, 16).toChar)
  }

  private def isLowSurrogate(digits: List[String]) = {
    Character.isLowSurrogate(Integer.parseInt(digits.mkString, 16).toChar)
  }

  def unicodeEscape = backslash ~ "u" ~ repN(4, hexDigit) ^^ {
    case _ ~ _ ~ digits =>
      Lit(UnicodeChar(Integer.parseInt(digits.mkString, 16)))
  }

  def hexEscape = backslash ~ "x" ~ repN(2, hexDigit) ^^ {
    case _ ~ _ ~ digits =>
      Lit(UnicodeChar(Integer.parseInt(digits.mkString, 16)))
  }

  def longHexEscape = backslash ~ "x" ~ "{" ~ hexDigit.+ ~ "}" ^^ {
    case _ ~ _ ~ digits ~ _ =>
      Lit(UnicodeChar(Integer.parseInt(digits.mkString, 16)))
  }

  def octalEscape = backslash ~ "0" ~ (repN(1, octalDigit) ||| repN(2, octalDigit) ||| repN(3, octalDigit)) ^^ {
    case _ ~ _ ~ digits =>
      Lit(UnicodeChar(Integer.parseInt(digits.mkString, 8)))
  }

  def controlEscape = (backslash ~ "c" ~ ".".r) ~>
    failure("Unsupported feature: control escape")

  def backReference= (backslash ~ "[1-9][0-9]*".r) ~>
    failure("unsupported feature: backreferences")

  def anchor = ("^" | "$") ~> failure("Unsupported feature: anchors")

  /**
   * Order between Unicode escapes is important
   */
  def anyEscape =
    specialEscape |
    doubleUnicodeEscape |
    unicodeEscape |
    hexEscape |
    longHexEscape |
    octalEscape |
    controlEscape |
    backReference

  def anythingExcept(parser: Parser[_]) = not(parser) ~> (".".r ^^ (x => Lit(UnicodeChar.fromSingletonString(x))))

  def quotedLiteral = backslash ~ "Q" ~ anythingExcept(backslash ~ "E").* ~ backslash ~ "E" ^^ {
    case _ ~ _ ~ literal ~ _ ~ _ => Juxt(literal)
  }

  def charLit = anchor | anythingExcept(charSpecial) | anyEscape

  def characterClassLit = anythingExcept(charSpecialInsideClasses) | anyEscape

  def singleCharacterClassLit = characterClassLit ^^ (lit => CharSet(Seq(lit)))

  def charClassRange = characterClassLit ~ "-" ~ characterClassLit ^^ {
    case start ~ _ ~ end => CharSet.fromRange(CharRange(start.char, end.char))
  }

  def specialCharSetByName = backslash ~ "p" ~ "{" ~ "[a-z_]+".r ~ "=" ~ "[a-zA-Z ]+".r ~ "}" ^^ {
    case _ ~ _ ~ propName ~ _ ~ propValue ~ _ =>
      if (propName == "block" || propName == "blk") {
        PredefinedCharSets.unicodeBlocks.getOrElse(propValue.toUpperCase(),
          throw new InvalidRegexException("Invalid Unicode block: " + propValue))
      } else if (propName == "script" || propName == "sc") {
        PredefinedCharSets.unicodeScripts.getOrElse(propValue.toUpperCase(),
          throw new InvalidRegexException("Invalid Unicode script: " + propValue))
      } else if (propName == "general_category" || propName == "gc") {
        PredefinedCharSets.unicodeGeneralCategories.getOrElse(propValue,
          throw new InvalidRegexException("Invalid Unicode general category: " + propValue))
      } else {
        throw new InvalidRegexException("Invalid Unicode character property name: " + propName)
      }
  }

  def specialCharSetWithIs = backslash ~ "p" ~ "{" ~ "Is" ~ "[a-zA-Z_ ]+".r ~ "}" ^^ {
    case _ ~ _ ~ _ ~ _ ~ name ~ _ =>
      /*
       * If the property starts with "Is" it could be either a script, 
       * general category or a binary property. Look for all.
       */
      PredefinedCharSets.unicodeScripts.get(name.toUpperCase()).orElse(
        PredefinedCharSets.unicodeGeneralCategories.get(name)).orElse(
          PredefinedCharSets.unicodeBinaryProperties.get(name.toUpperCase())).getOrElse {
            throw new InvalidRegexException("Invalid Unicode script, general category or binary property: " + name)
          }
  }

  def specialCharSetWithIn = backslash ~ "p" ~ "{" ~ "In" ~ "[a-zA-Z ]+".r ~ "}" ^^ {
    case _ ~ _ ~ _ ~ _ ~ blockName ~ _ =>
      PredefinedCharSets.unicodeBlocks.getOrElse(blockName.toUpperCase(),
        throw new InvalidRegexException("Invalid Unicode block: " + blockName))
  }

  def specialCharSetWithJava = backslash ~ "p" ~ "{" ~ "java" ~ "[a-zA-Z ]+".r ~ "}" ^^ {
    case _ ~ _ ~ _ ~ _ ~ charClass ~ _ =>
      PredefinedCharSets.javaClasses.getOrElse(charClass,
        throw new InvalidRegexException(
          s"invalid Java character class: $charClass " +
          s"(note: for such a class to be valid, a method java.lang.Character.is$charClass() must exist) " +
          s"(valid options: ${PredefinedCharSets.javaClasses.keys.toSeq.sorted.mkString(",")})"))
  }

  def specialCharSetImplicit = backslash ~ "p" ~ "{" ~ "[a-zA-Z ]+".r ~ "}" ^^ {
    case _ ~ _ ~ _ ~ name ~ _ =>
      PredefinedCharSets.posixClasses.get(name).orElse(
        PredefinedCharSets.unicodeGeneralCategories.get(name)).getOrElse {
          throw new InvalidRegexException("Invalid POSIX character class: " + name)
        }
  }

  def specialCharSet =
    specialCharSetByName |
    specialCharSetWithIs |
    specialCharSetWithIn |
    specialCharSetWithJava |
    specialCharSetImplicit

  def charClassAtom =
    charClassRange |
    singleCharacterClassLit |
    shorthandCharSet |
    specialCharSet

  def charClass = "[" ~ "^".? ~ "-".? ~ charClassAtom.+ ~ "-".? ~ "]" ^^ {
    case _ ~ negated ~ leftDash ~ charClass ~ rightDash ~ _ =>
      val chars = if (leftDash.isDefined || rightDash.isDefined)
        charClass :+ CharSet.fromRange(Lit('-'.u))
      else
        charClass
      val set = CharSet.fromCharSets(chars: _*)
      negated.fold[Node](set)(_ => set.complement)
  }

  // There is the special case of a character class with only one character: the dash. This is valid, but
  // not easily parsed by the general constructs.
  def dashClass = "[" ~ "^".? ~ "-" ~ "]" ^^ {
    case _ ~ negated ~ _ ~ _ =>
      val set = CharSet.fromRange(Lit('-'.u))
      negated.fold[CharSet](set)(_ => set.complement)
  }

  def shorthandCharSet = backslash ~ "[DWSdws]".r ^^ {
    case _ ~ "d" => PredefinedCharSets.digit
    case _ ~ "D" => PredefinedCharSets.digit.complement
    case _ ~ "s" => PredefinedCharSets.space
    case _ ~ "S" => PredefinedCharSets.space.complement
    case _ ~ "w" => PredefinedCharSets.wordChar
    case _ ~ "W" => PredefinedCharSets.wordChar.complement
    case _ => throw new AssertionError
  }

  def unicodeLineBreak = backslash ~ "R" ^^ {
    case _ =>
      Disj(Seq(
        Juxt(Seq(Lit('\u000D'.u), Lit('\u000A'.u))),
        Lit('\u000A'.u),
        Lit('\u000B'.u),
        Lit('\u000C'.u),
        Lit('\u000D'.u),
        Lit('\u0085'.u),
        Lit('\u2028'.u),
        Lit('\u2029'.u)
      ))
  }

  def group = "(" ~ ("?" ~ "<".? ~ "[:=!]".r).? ~ regex ~ ")" ^^ {
    case _ ~ modifiers ~ value ~ _ =>
      import Direction._
      import Condition._
      modifiers match {
        case None => PositionalCaptureGroup(value) // Naked parenthesis
        case Some(_ ~ None ~ ":") => value // Non-capturing group
        case Some(_ ~ None ~ "=") => Lookaround(Ahead, Positive, value)
        case Some(_ ~ None ~ "!") => Lookaround(Ahead, Negative, value)
        case Some(_ ~ Some("<") ~ ":") => throw new InvalidRegexException("Invalid grouping: <: ")
        case Some(_ ~ Some("<") ~ "=") => Lookaround(Behind, Positive, value)
        case Some(_ ~ Some("<") ~ "!") => Lookaround(Behind, Negative, value)
        case _ => throw new AssertionError
      }
  }

  def namedGroup = "(" ~ "?" ~ "<" ~ "[a-zA-Z][a-zA-Z0-9]*".r ~ ">" ~ regex ~ ")" ^^ {
    case _ ~ _ ~_ ~ name ~ _ ~ value ~ _ => NamedCaptureGroup(name, value)
  }

  def charWildcard = "." ^^^ Wildcard

  def regexAtom =
    quotedLiteral | charLit | charWildcard | charClass | unicodeLineBreak | dashClass | shorthandCharSet | specialCharSet | group | namedGroup

  case class Quantification(min: Int, max: Option[Int])

  def quantifier = predefQuantifier | generalQuantifier

  def predefQuantifier = ("+" | "*" | "?") ^^ {
    case "+" => Quantification(min = 1, max = None)
    case "*" => Quantification(min = 0, max = None)
    case "?" => Quantification(min = 0, max = Some(1))
  }

  def generalQuantifier = "{" ~ number ~ ("," ~ number.?).? ~ "}" ^^ {
    case _ ~ minVal ~ Some(comma ~ Some(maxVal)) ~ _ =>
      // Quantifiers of the for {min,max}
      if (minVal <= maxVal)
        Quantification(minVal, Some(maxVal))
      else
        throw new InvalidRegexException("invalid range in quantifier")
    case _ ~ minVal ~ Some(comma ~ None) ~ _ =>
      // Quantifiers of the form {min,}
      Quantification(minVal, None)
    case _ ~ minVal ~ None ~ _ =>
      // Quantifiers of the form "{n}", the value is captured as "min", despite being also the max
      Quantification(minVal, Some(minVal))
  }

  def lazyQuantifiedBranch = (regexAtom ~ quantifier ~ "?") ~>
    failure("reluctant quantifiers are not supported")

  def possesivelyQuantifiedBranch = (regexAtom ~ quantifier ~ "+") ~>
    failure("possessive quantifiers are not supported")

  def quantifiedBranch = regexAtom ~ quantifier ^^ {
    case atom ~ (q: Quantification) => Rep(min = q.min, max = q.max, value = atom)
    case _ ~ _ => throw new AssertionError()
  }

  def branch = (lazyQuantifiedBranch | possesivelyQuantifiedBranch | quantifiedBranch | regexAtom).+ ^^ {
    case Seq() => throw new AssertionError
    case Seq(first) => first
    case parts => Juxt(parts)
  }

  def emptyRegex = "" ^^^ Juxt(Seq())

  def nonEmptyRegex: Parser[Node] = branch ~ ("|" ~ regex).? ^^ {
    case left ~ Some(_ ~ right) => Disj(Seq(left, right))
    case left ~ None => left
  }

  def regex = nonEmptyRegex | emptyRegex

}

object RegexParser {

  def parse(regex: String): RegexTree.Node = {
    val parser = new RegexParser()
    parser.parseAll(parser.regex, regex) match {
      case parser.Success(ast, next) => ast
      case parser.NoSuccess((msg, next)) => throw new InvalidRegexException(msg)
    }
  }

}