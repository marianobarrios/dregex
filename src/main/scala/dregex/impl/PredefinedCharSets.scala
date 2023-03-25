package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.Lit
import dregex.impl.UnicodeChar.FromCharConversion
import dregex.impl.UnicodeChar.FromIntConversion
import dregex.impl.RegexTree.CharSet
import dregex.impl.RegexTree.CharRange

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

// [CROSS-BUILD] For immutable collections in Scala < 2.13
import scala.collection.immutable.Seq

// [CROSS-BUILD] For mapValues in Scala < 2.13
import scala.collection.compat._

object PredefinedCharSets {

  private[this] val logger = LoggerFactory.getLogger(PredefinedCharSets.getClass)

  val unicodeBlocks: Map[String, CharSet] = {
    val ret = collection.mutable.Map[String, CharSet]()
    for ((block, (from, to)) <- UnicodeDatabase.blocksRanges) {
      val charSet = CharSet.fromRange(CharRange(from.u, to.u))
      ret.put(UnicodeDatabaseReader.canonicalizeBlockName(block), charSet)
    }
    for ((block, alias) <- UnicodeDatabase.blockSynomyms) {
      ret.put(UnicodeDatabaseReader.canonicalizeBlockName(alias), ret(UnicodeDatabaseReader.canonicalizeBlockName(block)))
    }
    ret.toMap
  }

  val unicodeScripts: Map[String, CharSet] = {
    val ret = collection.mutable.Map[String, CharSet]()
    for ((block, ranges) <- UnicodeDatabase.scriptRanges) {
      val chatSet = CharSet(ranges.map(range => CharRange(range._1.u, range._2.u)))
      ret.put(block.toUpperCase, chatSet)
    }
    for ((script, alias) <- UnicodeDatabase.scriptSynomyms) {
      ret.put(alias.toUpperCase, ret(script.toUpperCase))
    }
    ret.toMap
  }

  val lower = CharSet.fromRange(CharRange(from = 'a'.u, to = 'z'.u))
  val upper = CharSet.fromRange(CharRange(from = 'A'.u, to = 'Z'.u))
  val alpha = CharSet.fromCharSets(lower, upper)
  val digit = CharSet.fromRange(CharRange(from = '0'.u, to = '9'.u))
  val alnum = CharSet.fromCharSets(alpha, digit)
  val punct = CharSet("""!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~""".map(char => Lit(UnicodeChar(char))))
  val graph = CharSet.fromCharSets(alnum, punct)
  val space = CharSet(Seq(Lit('\n'.u), Lit('\t'.u), Lit('\r'.u), Lit('\f'.u), Lit(' '.u), Lit(0x0B.u)))
  val wordChar = CharSet(alnum.ranges :+ Lit('_'.u))

  val posixClasses = Map(
    "Lower" -> lower,
    "Upper" -> upper,
    "ASCII" -> CharSet.fromRange(CharRange(from = 0.u, to = 0x7F.u)),
    "Alpha" -> alpha,
    "Digit" -> digit,
    "Alnum" -> alnum,
    "Punct" -> punct,
    "Graph" -> graph,
    "Print" -> CharSet(graph.ranges :+ Lit(0x20.u)),
    "Blank" -> CharSet(Seq(Lit(0x20.u), Lit('\t'.u))),
    "Cntrl" -> CharSet(Seq(CharRange(from = 0.u, to = 0x1F.u), Lit(0x7F.u))),
    "XDigit" -> CharSet(digit.ranges ++ Seq(CharRange(from = 'a'.u, to = 'f'.u), CharRange(from = 'A'.u, to = 'F'.u))),
    "Space" -> space
  )

  // Unicode version of POSIX-defined character classes

  val unicodeDigit = unicodeBinaryProperties("DIGIT")

  val unicodeSpace = unicodeBinaryProperties("WHITE_SPACE")

  val unicodeGraph = CharSet
    .fromCharSets(
      unicodeSpace,
      unicodeGeneralCategories("Cc"),
      unicodeGeneralCategories("Cs"),
      unicodeGeneralCategories("Cn")
    )
    .complement

  val unicodeBlank = CharSet(
    RangeOps.diff(
      unicodeSpace.ranges,
      unicodeGeneralCategories("Zl").ranges ++
        unicodeGeneralCategories("Zp").ranges ++
        Seq(CharRange(from = '\u000a'.u, to = '\u000d'.u)) ++ Seq(Lit('\u0085'.u))
    ))

  val unicodeWordChar = CharSet.fromCharSets(
    unicodeBinaryProperties("ALPHABETIC"),
    unicodeGeneralCategories("Mn"),
    unicodeGeneralCategories("Me"),
    unicodeGeneralCategories("Mc"),
    unicodeGeneralCategories("Mn"),
    unicodeDigit,
    unicodeGeneralCategories("Pc"),
    unicodeBinaryProperties("JOIN_CONTROL")
  )

  val unicodePosixClasses = Map(
    "Lower" -> unicodeBinaryProperties("LOWERCASE"),
    "Upper" -> unicodeBinaryProperties("UPPERCASE"),
    "ASCII" -> posixClasses("ASCII"),
    "Alpha" -> unicodeBinaryProperties("ALPHABETIC"),
    "Digit" -> unicodeDigit,
    "Alnum" -> CharSet.fromCharSets(unicodeBinaryProperties("ALPHABETIC"), unicodeDigit),
    "Punct" -> unicodeBinaryProperties("PUNCTUATION"),
    "Graph" -> unicodeGraph,
    "Print" -> CharSet(
      RangeOps
        .diff(CharSet.fromCharSets(unicodeGraph, unicodeBlank).ranges, unicodeGeneralCategories("Cc").ranges)),
    "Blank" -> unicodeBlank,
    "Cntrl" -> unicodeGeneralCategories("Cc"),
    "XDigit" -> CharSet.fromCharSets(unicodeGeneralCategories("Nd"), unicodeBinaryProperties("HEX_DIGIT")),
    "Space" -> unicodeSpace
  )

  /*
   * The following members are lazy val because collecting the categories and the properties
   * takes some time: only do it if used. This is because
   * we don't use a static definition, but iterate over all code points
   * and evaluate every property and the category.
   */

  lazy val allUnicodeLit: Seq[Lit] = {
    val (ret, elapsed) = Util.time {
      for (codePoint <- UnicodeChar.min.codePoint to UnicodeChar.max.codePoint) yield {
        Lit(codePoint.u)
      }
    }
    logger.debug(s"initialized ${ret.size} Unicode literals in $elapsed")
    ret
  }

  lazy val unicodeGeneralCategories: Map[String, CharSet] = {
    val (ret, elapsed) = Util.time {
      val builder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
      for (lit <- allUnicodeLit) {
        val categoryJavaId = Character.getType(lit.char.codePoint).toByte
        val category = GeneralCategory.categories(categoryJavaId)
        builder.getOrElseUpdate(category, ArrayBuffer()) += lit
        val parentCategory = category.substring(0, 1) // first letter
        builder.getOrElseUpdate(parentCategory, ArrayBuffer()) += lit
      }
      builder.view.mapValues(ranges => CharSet(RangeOps.union(ranges.to(Seq)))).toMap
    }
    logger.debug(s"initialized Unicode general category catalog in $elapsed")
    ret
  }

  lazy val unicodeBinaryProperties: Map[String, CharSet] = {
    logger.debug(s"initializing binary property catalog. This can take some time...")
    val (ret, elapsed) = Util.time {
      val builder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
      for {
        lit <- allUnicodeLit
        (prop, fn) <- GeneralCategory.binaryProperties
        if fn(lit.char.codePoint)
      } {
        builder.getOrElseUpdate(prop, ArrayBuffer()) += lit
      }
      builder.view.mapValues(ranges => CharSet(RangeOps.union(ranges.to(Seq)))).toMap
    }
    logger.debug(s"initialized binary property catalog in $elapsed")
    ret
  }

  lazy val javaClasses: Map[String, CharSet] = {
    val (ret, elapsed) = Util.time {
      val builder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
      for (lit <- allUnicodeLit) {
        for ((prop, fn) <- JavaCharacterProperties.properties if fn(lit.char.codePoint)) {
          builder.getOrElseUpdate(prop, ArrayBuffer()) += lit
        }
      }
      builder.view.mapValues(ranges => CharSet(RangeOps.union(ranges.to(Seq)))).toMap
    }
    logger.debug(s"initialized Java property catalog in $elapsed")
    ret
  }

}
