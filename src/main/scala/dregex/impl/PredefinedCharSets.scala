package dregex.impl

import dregex.impl.tree.{AbstractRange, CharRange, Lit, CharSet}
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

object PredefinedCharSets {

  private[this] val logger = LoggerFactory.getLogger(PredefinedCharSets.getClass)

  // Unicode version of POSIX-defined character classes

  val unicodeDigit = unicodeBinaryProperties("DIGIT")

  val unicodeSpace = unicodeBinaryProperties("WHITE_SPACE")

  val unicodeGraph = CharSet
    .fromCharSets(
      unicodeSpace,
      UnicodeGeneralCategories.unicodeGeneralCategories.get("Cc"),
      UnicodeGeneralCategories.unicodeGeneralCategories.get("Cs"),
      UnicodeGeneralCategories.unicodeGeneralCategories.get("Cn")
    )
    .complement

  val unicodeBlank = new CharSet(
    RangeOps.diff(
      unicodeSpace.ranges,
      (UnicodeGeneralCategories.unicodeGeneralCategories.get("Zl").ranges.asScala.toSeq ++
        UnicodeGeneralCategories.unicodeGeneralCategories.get("Zp").ranges.asScala.toSeq ++
        Seq(new CharRange('\u000a', '\u000d')) ++ Seq(new Lit('\u0085'))).asJava
    ).asJava)

  val unicodeWordChar = CharSet.fromCharSets(
    unicodeBinaryProperties("ALPHABETIC"),
    UnicodeGeneralCategories.unicodeGeneralCategories.get("Mn"),
    UnicodeGeneralCategories.unicodeGeneralCategories.get("Me"),
    UnicodeGeneralCategories.unicodeGeneralCategories.get("Mc"),
    UnicodeGeneralCategories.unicodeGeneralCategories.get("Mn"),
    unicodeDigit,
    UnicodeGeneralCategories.unicodeGeneralCategories.get("Pc"),
    unicodeBinaryProperties("JOIN_CONTROL")
  )

  val unicodePosixClasses = Map(
    "Lower" -> unicodeBinaryProperties("LOWERCASE"),
    "Upper" -> unicodeBinaryProperties("UPPERCASE"),
    "ASCII" -> PredefinedPosixCharSets.classes.get("ASCII"),
    "Alpha" -> unicodeBinaryProperties("ALPHABETIC"),
    "Digit" -> unicodeDigit,
    "Alnum" -> CharSet.fromCharSets(unicodeBinaryProperties("ALPHABETIC"), unicodeDigit),
    "Punct" -> unicodeBinaryProperties("PUNCTUATION"),
    "Graph" -> unicodeGraph,
    "Print" -> new CharSet(
      RangeOps.diff(CharSet.fromCharSets(unicodeGraph, unicodeBlank).ranges,
        UnicodeGeneralCategories.unicodeGeneralCategories.get("Cc").ranges).asJava),
    "Blank" -> unicodeBlank,
    "Cntrl" -> UnicodeGeneralCategories.unicodeGeneralCategories.get("Cc"),
    "XDigit" -> CharSet.fromCharSets(UnicodeGeneralCategories.unicodeGeneralCategories.get("Nd"), unicodeBinaryProperties("HEX_DIGIT")),
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
      new Lit(codePoint)
    }
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(s"initialized ${ret.size} Unicode literals in $elapsed")
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
    val ret = builder.view.mapValues(ranges => new CharSet(RangeOps.union(ranges.to(Seq)).asJava)).toMap
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
    val ret = builder.view.mapValues(ranges => new CharSet(RangeOps.union(ranges.to(Seq)).asJava)).toMap
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(s"initialized Java property catalog in $elapsed")
    ret
  }

}
