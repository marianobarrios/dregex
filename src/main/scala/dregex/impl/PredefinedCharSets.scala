package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.Lit
import dregex.impl.RegexTree.CharSet
import dregex.impl.RegexTree.CharRange
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

object PredefinedCharSets {

  private[this] val logger = LoggerFactory.getLogger(PredefinedCharSets.getClass)

  val unicodeBlocks: Map[String, CharSet] = {
    val ret = collection.mutable.Map[String, CharSet]()
    for ((block, range) <- UnicodeDatabase.blockRanges.asScala) {
      val charSet = CharSet.fromRange(CharRange(range.from, range.to))
      ret.put(UnicodeDatabaseReader.canonicalizeBlockName(block), charSet)
    }
    for ((block, alias) <- UnicodeDatabase.blockSynonyms.asScala) {
      ret.put(UnicodeDatabaseReader.canonicalizeBlockName(alias), ret(UnicodeDatabaseReader.canonicalizeBlockName(block)))
    }
    ret.toMap
  }

  val unicodeScripts: Map[String, CharSet] = {
    val ret = collection.mutable.Map[String, CharSet]()
    for ((block, ranges) <- UnicodeDatabase.scriptRanges.asScala) {
      val chatSet = CharSet(ranges.asScala.toSeq.map(range => CharRange(range.from, range.to)))
      ret.put(block.toUpperCase, chatSet)
    }
    for ((script, alias) <- UnicodeDatabase.scriptSynomyms.asScala) {
      ret.put(alias.toUpperCase, ret(script.toUpperCase))
    }
    ret.toMap
  }

  val lower = CharSet.fromRange(CharRange(from = 'a', to = 'z'))
  val upper = CharSet.fromRange(CharRange(from = 'A', to = 'Z'))
  val alpha = CharSet.fromCharSets(lower, upper)
  val digit = CharSet.fromRange(CharRange(from = '0', to = '9'))
  val alnum = CharSet.fromCharSets(alpha, digit)
  val punct = CharSet("""!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~""".map(char => Lit(char)))
  val graph = CharSet.fromCharSets(alnum, punct)
  val space = CharSet(Seq(Lit('\n'), Lit('\t'), Lit('\r'), Lit('\f'), Lit(' '), Lit(0x0B)))
  val wordChar = CharSet(alnum.ranges :+ Lit('_'))

  val posixClasses = Map(
    "Lower" -> lower,
    "Upper" -> upper,
    "ASCII" -> CharSet.fromRange(CharRange(from = 0, to = 0x7F)),
    "Alpha" -> alpha,
    "Digit" -> digit,
    "Alnum" -> alnum,
    "Punct" -> punct,
    "Graph" -> graph,
    "Print" -> CharSet(graph.ranges :+ Lit(0x20)),
    "Blank" -> CharSet(Seq(Lit(0x20), Lit('\t'))),
    "Cntrl" -> CharSet(Seq(CharRange(from = 0, to = 0x1F), Lit(0x7F))),
    "XDigit" -> CharSet(digit.ranges ++ Seq(CharRange(from = 'a', to = 'f'), CharRange(from = 'A', to = 'F'))),
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
        Seq(CharRange(from = '\u000a', to = '\u000d')) ++ Seq(Lit('\u0085'))
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
    val start = System.nanoTime()
    val ret = for (codePoint <- Character.MIN_CODE_POINT to Character.MAX_CODE_POINT) yield {
      Lit(codePoint)
    }
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(s"initialized ${ret.size} Unicode literals in $elapsed")
    ret
  }

  lazy val unicodeGeneralCategories: Map[String, CharSet] = {
    val start = System.nanoTime()
    val builder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
    for (lit <- allUnicodeLit) {
      val categoryJavaId = Character.getType(lit.codePoint).toByte
      val category = GeneralCategory.categories.get(categoryJavaId)
      builder.getOrElseUpdate(category, ArrayBuffer()) += lit
      val parentCategory = category.substring(0, 1) // first letter
      builder.getOrElseUpdate(parentCategory, ArrayBuffer()) += lit
    }
    val ret = builder.view.mapValues(ranges => CharSet(RangeOps.union(ranges.to(Seq)))).toMap
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(s"initialized Unicode general category catalog in $elapsed")
    ret
  }

  lazy val unicodeBinaryProperties: Map[String, CharSet] = {
    logger.debug(s"initializing binary property catalog. This can take some time...")
    val start = System.nanoTime()
    val builder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
    for {
      lit <- allUnicodeLit
      (prop, fn) <- GeneralCategory.binaryProperties.asScala
      if fn.test(lit.codePoint)
    } {
      builder.getOrElseUpdate(prop, ArrayBuffer()) += lit
    }
    val ret = builder.view.mapValues(ranges => CharSet(RangeOps.union(ranges.to(Seq)))).toMap
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(s"initialized binary property catalog in $elapsed")
    ret
  }

  lazy val javaClasses: Map[String, CharSet] = {
    val start = System.nanoTime()
    val builder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
    for (lit <- allUnicodeLit) {
      for ((prop, fn) <- JavaCharacterProperties.properties if fn(lit.codePoint)) {
        builder.getOrElseUpdate(prop, ArrayBuffer()) += lit
      }
    }
    val ret = builder.view.mapValues(ranges => CharSet(RangeOps.union(ranges.to(Seq)))).toMap
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(s"initialized Java property catalog in $elapsed")
    ret
  }

}
