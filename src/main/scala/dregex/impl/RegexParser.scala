package dregex.impl

import scala.util.parsing.combinator.JavaTokenParsers
import dregex.InvalidRegexException
import dregex.impl.UnicodeChar.FromCharConversion
import scala.collection.immutable.Seq

class RegexParser extends JavaTokenParsers {

  override def skipWhitespace = false

  import RegexTree._

  // Atoms (strings and regexes)

  def backslash = """\"""
  def hexDigit = """\p{XDigit}""".r
  def octalDigit = "[0-7]".r
  def decimalDigit = """\d""".r

  // Parsers that return a primitive (string, number)

  def hexNumber(digitCount: Int) = repN(digitCount, hexDigit) ^^ { digits =>
    Integer.parseInt(digits.mkString, 16)
  }

  def hexNumber = hexDigit.+ ^^ { digits =>
    Integer.parseInt(digits.mkString, 16)
  }

  def octalNumber(digitCount: Int) = repN(digitCount, octalDigit) ^^ { digits =>
    Integer.parseInt(digits.mkString, 8)
  }

  def number = decimalDigit.+ ^^ { digits =>
    try {
      Integer.parseInt(digits.mkString)
    } catch {
      case _: NumberFormatException => throw new InvalidRegexException("Cannot parse number: " + digits.mkString)
    }
  }

  def charSpecialInsideClasses = backslash | "]" | "^" | "-"
  def charSpecial = backslash | "." | "|" | "(" | ")" | "[" | "]" | "+" | "*" | "?" | "^" | "$"

  def controlEscape = backslash ~ "c" ~ ".".r ~>
    failure("Unsupported feature: control escape")

  def backReference= backslash ~ "[1-9][0-9]*".r ~>
    failure("unsupported feature: backreferences")

  def anchor = ("^" | "$") ~> failure("Unsupported feature: anchors")

  // Parsers that return a literal Node

  def specialEscape = backslash ~> "[^dwsDWSuxcpR0123456789]".r ^^ {
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

  def doubleUnicodeEscape = backslash ~ "u" ~ hexNumber(4) ~ backslash ~ "u" ~ hexNumber(4) ^? {
    case _ ~ _ ~ highNumber ~ _ ~ _ ~ lowNumber if Character.isHighSurrogate(highNumber.toChar) && Character.isLowSurrogate(lowNumber.toChar) =>
      val codePoint = Character.toCodePoint(highNumber.toChar, lowNumber.toChar)
      Lit(UnicodeChar(codePoint))
  }

  def unicodeEscape = backslash ~ "u" ~> hexNumber(4) ^^ { codePoint =>
    Lit(UnicodeChar(codePoint))
  }

  def hexEscape = backslash ~ "x" ~> hexNumber(2) ^^ { codePoint =>
    Lit(UnicodeChar(codePoint))
  }

  def longHexEscape = backslash ~ "x" ~ "{" ~> hexNumber <~ "}" ^^ { codePoint =>
    Lit(UnicodeChar(codePoint))
  }

  def octalEscape = backslash ~ "0" ~> (octalNumber(1) ||| octalNumber(2) ||| octalNumber(3)) ^^ { codePoint =>
    Lit(UnicodeChar(codePoint))
  }

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

  def charLit = anchor | anythingExcept(charSpecial) | anyEscape

  def characterClassLit = anythingExcept(charSpecialInsideClasses) | anyEscape

  // Parsers that return a character class Node

  def singleCharacterClassLit = characterClassLit ^^ (lit => CharSet(Seq(lit)))

  def charClassRange = characterClassLit ~ "-" ~ characterClassLit ^^ {
    case start ~ _ ~ end => CharSet.fromRange(CharRange(start.char, end.char))
  }

  def specialCharSetByName = backslash ~ "p" ~ "{" ~> "[a-z_]+".r ~ "=" ~ "[a-zA-Z ]+".r <~ "}" ^^ {
    case propName ~ _ ~ propValue =>
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

  def specialCharSetWithIs = backslash ~ "p" ~ "{" ~ "Is" ~> "[a-zA-Z_ ]+".r <~ "}" ^^ { name =>
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

  def specialCharSetWithIn = backslash ~ "p" ~ "{" ~ "In" ~> "[a-zA-Z ]+".r <~ "}" ^^ { blockName =>
    PredefinedCharSets.unicodeBlocks.getOrElse(blockName.toUpperCase(),
      throw new InvalidRegexException("Invalid Unicode block: " + blockName))
  }

  def specialCharSetWithJava = backslash ~ "p" ~ "{" ~ "java" ~> "[a-zA-Z ]+".r <~ "}" ^^ { charClass =>
    PredefinedCharSets.javaClasses.getOrElse(charClass,
      throw new InvalidRegexException(
        s"invalid Java character class: $charClass " +
        s"(note: for such a class to be valid, a method java.lang.Character.is$charClass() must exist) " +
        s"(valid options: ${PredefinedCharSets.javaClasses.keys.toSeq.sorted.mkString(",")})"))
  }

  def specialCharSetImplicit = backslash ~ "p" ~ "{" ~> "[a-zA-Z ]+".r <~ "}" ^^ { name  =>
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

  // There is the special case of a character class with only one character: the dash. This is valid, but
  // not easily parsed by the general constructs.
  def dashClass = "[" ~> "^".? <~ "-" ~ "]" ^^ { negated =>
    val set = CharSet.fromRange(Lit('-'.u))
    if (negated.isDefined) {
      set.complement
    } else {
      set
    }
  }

  def shorthandCharSet = backslash ~> "[DWSdws]".r ^^ {
    case "d" => PredefinedCharSets.digit
    case "D" => PredefinedCharSets.digit.complement
    case "s" => PredefinedCharSets.space
    case "S" => PredefinedCharSets.space.complement
    case "w" => PredefinedCharSets.wordChar
    case "W" => PredefinedCharSets.wordChar.complement
    case _ => throw new AssertionError
  }

  def charClass = "[" ~> "^".? ~ "-".? ~ charClassAtom.+ ~ "-".? <~ "]" ^^ {
    case negated ~ leftDash ~ charClass ~ rightDash =>
      val chars = if (leftDash.isDefined || rightDash.isDefined)
        charClass :+ CharSet.fromRange(Lit('-'.u))
      else
        charClass
      val set = CharSet.fromCharSets(chars: _*)
      if (negated.isDefined) {
        set.complement
      } else {
        set
      }
  }

  // Parsers that return a complex Node

  def quotedLiteral = backslash ~ "Q" ~> anythingExcept(backslash ~ "E").* <~ backslash ~ "E" ^^ {
    literal => Juxt(literal)
  }

  def unicodeLineBreak = backslash ~ "R" ^^^ {
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

  def group = "(" ~> ("?" ~ "<".? ~ "[:=!]".r).? ~ regex <~ ")" ^^ {
    case modifiers ~ value =>
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

  def namedGroup = "(" ~ "?" ~ "<" ~> "[a-zA-Z][a-zA-Z0-9]*".r ~ ">" ~ regex <~ ")" ^^ {
    case name ~ _ ~ value => NamedCaptureGroup(name, value)
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

  def generalQuantifier = "{" ~> number ~ ("," ~ number.?).? <~ "}" ^^ {
    case minVal ~ Some(comma ~ Some(maxVal)) =>
      // Quantifiers of the for {min,max}
      if (minVal <= maxVal)
        Quantification(minVal, Some(maxVal))
      else
        throw new InvalidRegexException("invalid range in quantifier")
    case minVal ~ Some(comma ~ None) =>
      // Quantifiers of the form {min,}
      Quantification(minVal, None)
    case minVal ~ None =>
      // Quantifiers of the form "{n}", the value is captured as "min", despite being also the max
      Quantification(minVal, Some(minVal))
  }

  def lazyQuantifiedBranch = (regexAtom ~ quantifier ~ "?") ~>
    failure("reluctant quantifiers are not supported")

  def possesivelyQuantifiedBranch = (regexAtom ~ quantifier ~ "+") ~>
    failure("possessive quantifiers are not supported")

  def quantifiedBranch = regexAtom ~ quantifier ^^ {
    case atom ~ (q: Quantification) => Rep(min = q.min, max = q.max, value = atom)
  }

  def branch = (lazyQuantifiedBranch | possesivelyQuantifiedBranch | quantifiedBranch | regexAtom).+ ^^ {
    case Seq() => throw new AssertionError
    case Seq(first) => first
    case parts => Juxt(parts)
  }

  def emptyRegex = "" ^^^ Juxt(Seq())

  def nonEmptyRegex: Parser[Node] = branch ~ ("|" ~> regex).? ^^ {
    case left ~ Some(right) => Disj(Seq(left, right))
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