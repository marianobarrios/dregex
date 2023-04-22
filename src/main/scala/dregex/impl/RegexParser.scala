package dregex.impl

import java.util.regex.Pattern
import dregex.{InvalidRegexException, ParsedRegex}
import dregex.impl.RegexParser.DotMatch

import scala.util.parsing.combinator.RegexParsers
import scala.jdk.CollectionConverters._
import dregex.impl.tree.{CharRange, CharSet, Condition, Direction, Disj, Lit, Lookaround, NamedCaptureGroup, Node, PositionalCaptureGroup, Rep, Juxt, Wildcard}

import java.util.Optional

class RegexParser(comments: Boolean, dotMatch: DotMatch, unicodeClasses: Boolean) extends RegexParsers {

  override def skipWhitespace = false

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

  def controlEscape =
    backslash ~ "c" ~ ".".r ~>
      failure("Unsupported feature: control escape")

  def backReference =
    backslash ~ "[1-9][0-9]*".r ~>
      failure("unsupported feature: backreferences")

  def anchor = ("^" | "$") ~> failure("Unsupported feature: anchors")

  /**
    * Special ignorable space, only enabled by a special parameter.
    */
  def sp = {
    if (comments) {
      """\s*""".r ^^^ None // ASCII white space intentionally for Java compatibility
    } else {
      "" ^^^ None
    }
  }

  // Parsers that return a literal Node

  def specialEscape = backslash ~> "[^dwsDWSuxcpR0123456789]".r ^^ {
    case "n" => new Lit('\n')
    case "r" => new Lit('\r')
    case "t" => new Lit('\t')
    case "f" => new Lit('\f')
    case "b" => new Lit('\b')
    case "v" => new Lit('\u000B') // vertical tab
    case "a" => new Lit('\u0007') // bell
    case "e" => new Lit('\u001B') // escape
    case "B" => new Lit('\\')
    case c   => Lit.fromSingletonString(c) // remaining escaped characters stand for themselves
  }

  def doubleUnicodeEscape = backslash ~ "u" ~ hexNumber(4) ~ backslash ~ "u" ~ hexNumber(4) ^? {
    case _ ~ _ ~ highNumber ~ _ ~ _ ~ lowNumber
        if Character.isHighSurrogate(highNumber.toChar) && Character.isLowSurrogate(lowNumber.toChar) =>
      val codePoint = Character.toCodePoint(highNumber.toChar, lowNumber.toChar)
      new Lit(codePoint)
  }

  def unicodeEscape = backslash ~ "u" ~> hexNumber(4) ^^ { codePoint =>
    new Lit(codePoint)
  }

  def hexEscape = backslash ~ "x" ~> hexNumber(2) ^^ { codePoint =>
    new Lit(codePoint)
  }

  def longHexEscape = backslash ~ "x" ~ "{" ~> hexNumber <~ "}" ^^ { codePoint =>
    new Lit(codePoint)
  }

  def octalEscape = backslash ~ "0" ~> (octalNumber(1) ||| octalNumber(2) ||| octalNumber(3)) ^^ { codePoint =>
    new Lit(codePoint)
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

  def anythingExcept(parser: Parser[_]) = not(parser) ~> (".".r ^^ (x => Lit.fromSingletonString(x)))

  def charLit = anchor | anythingExcept(charSpecial) | anyEscape

  def characterClassLit = anythingExcept(charSpecialInsideClasses) | anyEscape

  // Parsers that return a character class Node

  def singleCharacterClassLit = characterClassLit ^^ (lit => new CharSet(java.util.List.of(lit)))

  def charClassRange = characterClassLit ~ "-" ~ characterClassLit ^^ {
    case start ~ _ ~ end => CharSet.fromRange(new CharRange(start.codePoint, end.codePoint))
  }

  private val unicodeSubsetName = "[0-9a-zA-Z_ -]+".r

  def specialCharSetByName = backslash ~ "p" ~ "{" ~> "[a-z_]+".r ~ "=" ~ unicodeSubsetName <~ "}" ^^ {
    case propName ~ _ ~ propValue =>
      if (propName == "block" || propName == "blk") {
        val canonicalBlockName = UnicodeDatabaseReader.canonicalizeBlockName(propValue)
        val block = UnicodeBlocks.unicodeBlocks.get(canonicalBlockName)
        if (block == null) {
          throw new InvalidRegexException("Invalid Unicode block: " + propValue)
        }
        block
      } else if (propName == "script" || propName == "sc") {
        val script = UnicodeScripts.unicodeScripts.get(propValue.toUpperCase())
        if (script == null) {
          throw new InvalidRegexException("Invalid Unicode script: " + propValue)
        }
        script
      } else if (propName == "general_category" || propName == "gc") {
        val gc = UnicodeGeneralCategories.unicodeGeneralCategories.get(propValue)
        if (gc == null) {
          throw new InvalidRegexException("Invalid Unicode general category: " + propValue)
        }
        gc
      } else {
        throw new InvalidRegexException("Invalid Unicode character property name: " + propName)
      }
  }

  def specialCharSetWithIs = backslash ~ "p" ~ "{" ~ "Is" ~> unicodeSubsetName <~ "}" ^^ { name =>
    /*
     * If the property starts with "Is" it could be either a script,
     * general category or a binary property. Look for all.
     */
    UnicodeScripts.unicodeScripts.asScala
      .get(name.toUpperCase())
      .orElse(Option(UnicodeGeneralCategories.unicodeGeneralCategories.get(name)))
      .orElse(Option(UnicodeBinaryProperties.unicodeBinaryProperties.get(name.toUpperCase())))
      .getOrElse {
        throw new InvalidRegexException("Invalid Unicode script, general category or binary property: " + name)
      }
  }

  def specialCharSetWithIn = backslash ~ "p" ~ "{" ~ "In" ~> unicodeSubsetName <~ "}" ^^ { blockName =>
    val block = UnicodeBlocks.unicodeBlocks.get(UnicodeDatabaseReader.canonicalizeBlockName(blockName))
    if (block == null) {
      throw new InvalidRegexException("Invalid Unicode block: " + blockName)
    }
    block
  }

  def specialCharSetWithJava = backslash ~ "p" ~ "{" ~ "java" ~> unicodeSubsetName <~ "}" ^^ { charClass =>
    val ret = PredefinedJavaProperties.javaClasses.get(charClass);
    if (ret == null) {
      throw new InvalidRegexException(
        s"invalid Java character class: $charClass " +
          s"(note: for such a class to be valid, a method java.lang.Character.is$charClass() must exist) " +
          s"(valid options: ${PredefinedJavaProperties.javaClasses.keySet().asScala.toSeq.sorted.mkString(",")})")
    }
    ret
  }

  def specialCharSetImplicit = backslash ~ "p" ~ "{" ~> unicodeSubsetName <~ "}" ^^ { name =>
    val effPosixClasses = {
      if (unicodeClasses) {
        PredefinedUnicodePosixCharSets.unicodePosixClasses.asScala
      } else {
        PredefinedPosixCharSets.classes.asScala
      }
    }
    effPosixClasses.get(name).orElse(Option(UnicodeGeneralCategories.unicodeGeneralCategories.get(name))).getOrElse {
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
    val set = CharSet.fromRange(new Lit('-'))
    if (negated.isDefined) {
      set.complement
    } else {
      set
    }
  }

  def shorthandCharSet =
    shorthandCharSetDigit |
      shorthandCharSetDigitCompl |
      shorthandCharSetSpace |
      shorthandCharSetSpaceCompl |
      shorthandCharSetWord |
      shorthandCharSetWordCompl

  def shorthandCharSetDigit = backslash ~ "d" ^^^ {
    if (unicodeClasses)
      UnicodeBinaryProperties.unicodeBinaryProperties.get("DIGIT")
    else
      PredefinedPosixCharSets.digit
  }

  def shorthandCharSetDigitCompl = backslash ~ "D" ^^^ {
    if (unicodeClasses)
      UnicodeBinaryProperties.unicodeBinaryProperties.get("DIGIT").complement
    else
      PredefinedPosixCharSets.digit.complement
  }

  def shorthandCharSetSpace = backslash ~ "s" ^^^ {
    if (unicodeClasses)
      UnicodeBinaryProperties.unicodeBinaryProperties.get("WHITE_SPACE")
    else
      PredefinedPosixCharSets.space
  }

  def shorthandCharSetSpaceCompl = backslash ~ "S" ^^^ {
    if (unicodeClasses)
      UnicodeBinaryProperties.unicodeBinaryProperties.get("WHITE_SPACE").complement
    else
      PredefinedPosixCharSets.space.complement
  }

  def shorthandCharSetWord = backslash ~ "w" ^^^ {
    if (unicodeClasses)
      PredefinedUnicodePosixCharSets.unicodeWordChar
    else
      PredefinedPosixCharSets.wordChar
  }

  def shorthandCharSetWordCompl = backslash ~ "W" ^^^ {
    if (unicodeClasses)
      PredefinedUnicodePosixCharSets.unicodeWordChar.complement
    else
      PredefinedPosixCharSets.wordChar.complement
  }

  def charClass = "[" ~> "^".? ~ "-".? ~ charClassAtom.+ ~ "-".? <~ "]" ^^ {
    case negated ~ leftDash ~ charClass ~ rightDash =>
      val chars =
        if (leftDash.isDefined || rightDash.isDefined)
          charClass :+ CharSet.fromRange(new Lit('-'))
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

  def quotedLiteral = backslash ~ "Q" ~> anythingExcept(backslash ~ "E").* <~ backslash ~ "E" ^^ { literal =>
    new Juxt(literal.asJava)
  }

  def unicodeLineBreak = backslash ~ "R" ^^^ {
    new Disj(java.util.List.of(new Juxt(java.util.List.of(
      new Lit('\u000D'), new Lit('\u000A'))), new Lit('\u000A'),
      new Lit('\u000B'), new Lit('\u000C'), new Lit('\u000D'), new Lit('\u0085'),
      new Lit('\u2028'), new Lit('\u2029')))
  }

  def group = "(" ~> ("?" ~ "<".? ~ "[:=!]".r).? ~ sp ~ regex <~ sp ~ ")" ^^ {
    case modifiers ~ _ ~ value =>
      import Direction._
      import Condition._
      modifiers match {
        case None                      => new PositionalCaptureGroup(value) // Naked parenthesis
        case Some(_ ~ None ~ ":")      => value // Non-capturing group
        case Some(_ ~ None ~ "=")      => new Lookaround(Ahead, Positive, value)
        case Some(_ ~ None ~ "!")      => new Lookaround(Ahead, Negative, value)
        case Some(_ ~ Some("<") ~ ":") => throw new InvalidRegexException("Invalid grouping: <: ")
        case Some(_ ~ Some("<") ~ "=") => new Lookaround(Behind, Positive, value)
        case Some(_ ~ Some("<") ~ "!") => new Lookaround(Behind, Negative, value)
        case _                         => throw new AssertionError
      }
  }

  def namedGroup = "(" ~ "?" ~ "<" ~> "[a-zA-Z][a-zA-Z0-9]*".r ~ ">" ~ regex <~ ")" ^^ {
    case name ~ _ ~ value => new NamedCaptureGroup(name, value)
  }

  def charWildcard = "." ^^^ {
    dotMatch match {
      case DotMatch.All =>
        Wildcard.instance
      case DotMatch.JavaLines =>
        new CharSet(java.util.List.of(new Lit('\n'), new Lit('\r'), new Lit('\u0085'), new Lit('\u2028'),
          new Lit('\u2829'))).complement
      case DotMatch.UnixLines =>
        CharSet.fromRange(new Lit('\n')).complement
    }
  }

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

  def lazyQuantifiedBranch =
    (regexAtom ~ quantifier ~ "?") ~>
      failure("reluctant quantifiers are not supported")

  def possesivelyQuantifiedBranch =
    (regexAtom ~ quantifier ~ "+") ~>
      failure("possessive quantifiers are not supported")

  def quantifiedBranch = regexAtom ~ sp ~ quantifier ^^ {
    case atom ~ _ ~ (q: Quantification) =>
      val javaOptional = q.max match {
        case Some(n) => Optional.of[Integer](n)
        case None => Optional.empty[Integer]()
      }
      new Rep(q.min, javaOptional, atom)
  }

  def branch = ((lazyQuantifiedBranch | possesivelyQuantifiedBranch | quantifiedBranch | regexAtom) <~ sp).+ ^^ {
    case Seq()      => throw new AssertionError
    case Seq(first) => first
    case parts      => new Juxt(parts.asJava)
  }

  def emptyRegex = "" ^^^ new Juxt(java.util.List.of())

  def nonEmptyRegex: Parser[Node] = sp ~> branch ~ (sp ~ "|" ~ sp ~> regex).? ^^ {
    case left ~ Some(right) => new Disj(java.util.List.of(left, right))
    case left ~ None        => left
  }

  def regex = nonEmptyRegex | emptyRegex

}

object RegexParser {

  private val commentPattern = Pattern.compile("""(?<!\\)#[^\n]*""")

  private val embeddedFlagPattern = Pattern.compile("""\(\?([a-z]*)\)""")

  sealed trait DotMatch
  object DotMatch {
    case object All extends DotMatch
    case object JavaLines extends DotMatch
    case object UnixLines extends DotMatch
  }

  case class Flags(
      var dotMatch: DotMatch = DotMatch.All,
      literal: Boolean = false,
      var comments: Boolean = false,
      var unicodeClasses: Boolean = false,
      var caseInsensitive: Boolean = false,
      var unicodeCase: Boolean = false,
      canonicalEq: Boolean = false,
      var multiline: Boolean = false
  )

  def parse(regex: String, flags: Flags = Flags()): ParsedRegex = {
    if (flags.literal) {
      parseLiteralRegex(regex)
    } else {
      // process embedded flags
      var effRegex = regex
      val matcher = embeddedFlagPattern.matcher(regex)
      while (matcher.find()) {
        if (matcher.start > 0) {
          throw new InvalidRegexException(s"embedded flag are only valid at the beginning of the pattern")
        }
        for (flag <- matcher.group(1)) {
          flag match {
            case 'x' => flags.comments = true
            case 's' => flags.dotMatch = DotMatch.All
            case 'd' => flags.dotMatch = DotMatch.UnixLines
            case 'U' => flags.unicodeClasses = true
            case 'i' => flags.caseInsensitive = true
            case 'u' => flags.unicodeCase = true
            case 'm' => flags.multiline = true
            case c   => throw new InvalidRegexException(s"invalid embedded flag: $c")
          }
          effRegex = effRegex.substring(matcher.end)
        }
      }
      if (flags.multiline) {
        throw new InvalidRegexException("multiline flag is not supported; this class always works in multiline mode")
      }

      // replace comments
      if (flags.comments) {
        effRegex = commentPattern.matcher(effRegex).replaceAll(" ")
      }
      parseRegexImpl(effRegex, flags)
    }
  }

  /**
    * Parse a quoted regex. They don't really need parsing.
    */
  private def parseLiteralRegex(regex: String): ParsedRegex = {
    val literals: Seq[Lit] = regex.map { char =>
      new Lit(char)
    }
    new ParsedRegex(regex, new Juxt(literals.asJava), Normalization.NoNormalization)
  }

  /**
    * Parse an actual regex that is not a literal.
    */
  private def parseRegexImpl(regex: String, flags: Flags): ParsedRegex = {
    // normalize case
    var normalizer: Normalizer = if (flags.caseInsensitive) {
      if (flags.unicodeClasses | flags.unicodeCase) {
        Normalization.UnicodeLowerCase
      } else {
        Normalization.LowerCase
      }
    } else {
      Normalization.NoNormalization
    }

    if (flags.canonicalEq) {
      normalizer = Normalizer.combine(Normalization.CanonicalDecomposition, normalizer)
    }

    // parsing proper
    val parser = new RegexParser(flags.comments, flags.dotMatch, flags.unicodeClasses)

    val tree: Node = parser.parseAll(parser.regex, normalizer.normalize(regex)) match {
      case parser.Success(ast, next)     => ast
      case parser.NoSuccess((msg, next)) => throw new InvalidRegexException(msg)
    }

    new ParsedRegex(regex, tree, normalizer)
  }

}
