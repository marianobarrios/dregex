package dregex.impl

import dregex.impl.tree.{AbstractRange, Lit, CharSet}
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

object PredefinedCharSets {

  private[this] val logger = LoggerFactory.getLogger(PredefinedCharSets.getClass)

  val javaClasses: Map[String, CharSet] = {
    val start = System.nanoTime()
    val builder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
    for (codePoint <- Character.MIN_CODE_POINT to Character.MAX_CODE_POINT) {
      for ((prop, fn) <- JavaCharacterProperties.properties if fn(codePoint)) {
        builder.getOrElseUpdate(prop, ArrayBuffer()) += new Lit(codePoint)
      }
    }
    val ret = builder.view.mapValues(ranges => new CharSet(RangeOps.union(ranges.to(Seq)).asJava)).toMap
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    logger.debug(s"initialized Java property catalog in $elapsed")
    ret
  }

}
