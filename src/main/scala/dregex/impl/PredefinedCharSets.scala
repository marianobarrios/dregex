package dregex.impl

import dregex.impl.tree.{AbstractRange, CharRange, Lit, CharSet}
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

object PredefinedCharSets {

  private[this] val logger = LoggerFactory.getLogger(PredefinedCharSets.getClass)

  /*
   * The following members are lazy val because collecting the categories and the properties
   * takes some time: only do it if used. This is because
   * we don't use a static definition, but iterate over all code points
   * and evaluate every property and the category.
   */

  private val allUnicodeLit: Seq[Lit] = {
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

  val javaClasses: Map[String, CharSet] = {
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
