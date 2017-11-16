package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.Lit
import dregex.impl.UnicodeChar.FromCharConversion
import dregex.impl.UnicodeChar.FromIntConversion
import dregex.impl.RegexTree.CharSet
import dregex.impl.RegexTree.CharRange
import java.lang.Character.UnicodeBlock

import scala.collection.breakOut
import java.lang.Character.UnicodeScript

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.immutable.Seq

object PredefinedCharSets {

  private[this] val logger = LoggerFactory.getLogger(PredefinedCharSets.getClass)

  val unicodeBlocks: Map[String, CharSet] = {
    val blockStarts = Util.getPrivateStaticField[Array[Int]](classOf[UnicodeBlock], "blockStarts")
    val javaBlocks = Util.getPrivateStaticField[Array[UnicodeBlock]](classOf[UnicodeBlock], "blocks").toSeq
    val blockToSetMap: Map[UnicodeBlock, CharSet] = blockStarts.indices.flatMap { i =>
      val from = blockStarts(i)
      val to = if (i == blockStarts.length - 1)
        UnicodeChar.max.codePoint
      else
        blockStarts(i + 1)
      // skip unassigned blocks
      javaBlocks.lift(i).map { block =>
        block -> CharSet.fromRange(CharRange(from.u, to.u))
      }
    }(breakOut)
    val alias =
      Util.getPrivateStaticField[java.util.Map[String, UnicodeBlock]](classOf[UnicodeBlock], "map").asScala.toMap
    alias.mapValues { javaUnicodeBlock =>
      /*
       * As of Java 1.8, there exists one deprecated block (UnicodeBlock.SURROGATES_AREA)
       * that doesn't have any range assigned. Respect Java behavior and make it match nothing.
       */
      blockToSetMap.getOrElse(javaUnicodeBlock, CharSet(Seq()))
    }
  }

  val unicodeScripts: Map[String, CharSet] = {
    val scriptStarts = Util.getPrivateStaticField[Array[Int]](classOf[UnicodeScript], "scriptStarts")
    val javaScripts = Util.getPrivateStaticField[Array[UnicodeScript]](classOf[UnicodeScript], "scripts").toSeq
    val scriptToSetMap = {
      val builder = collection.mutable.Map[UnicodeScript, CharSet]()
      for (i <- 0 until scriptStarts.length) {
        val from = scriptStarts(i)
        val to = if (i == scriptStarts.length - 1)
          UnicodeChar.max.codePoint
        else
          scriptStarts(i + 1)
        // skip unassigned scripts
        javaScripts.lift(i).foreach { script =>
          val CharSet(existing) = builder.getOrElse(script, CharSet(Seq()))
          builder.put(script, CharSet(existing :+ CharRange(from.u, to.u)))
        }
      }
      builder.toMap
    }
    val aliases =
      Util.getPrivateStaticField[java.util.Map[String, UnicodeScript]](classOf[UnicodeScript], "aliases").asScala.toMap
    val canonicalNames = scriptToSetMap.map {
      case (script, charSet) =>
        (script.name(), charSet)
    }
    canonicalNames ++ aliases.mapValues(scriptToSetMap)
  }

  /*
   * Use a lazy val because collecting the categories and the properties 
   * takes some time: only do it if used. This is because
   * we don't use a static definition, but iterate over all code points 
   * and evaluate every property and the category.
   */
  lazy val (unicodeGeneralCategories, unicodeBinaryProperties): (Map[String, CharSet], Map[String, CharSet]) = {
    val (ret, elapsed) = Util.time {
      val categoryBuilder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
      val propertyBuilder = collection.mutable.Map[String, ArrayBuffer[AbstractRange]]()
      for (codePoint <- UnicodeChar.min.codePoint to UnicodeChar.max.codePoint) {
        
        val char = Lit(codePoint.u)

        // category
        val categoryJavaId = Character.getType(codePoint).toByte
        val category = GeneralCategory.categories(categoryJavaId)
        categoryBuilder.getOrElseUpdate(category, ArrayBuffer()) += char
        val parentCategory = category.substring(0, 1) // first letter
        categoryBuilder.getOrElseUpdate(parentCategory, ArrayBuffer()) += char
        
        // properties
        for ((prop, fn) <- GeneralCategory.binaryProperties if fn(codePoint)) {
          propertyBuilder.getOrElseUpdate(prop, ArrayBuffer()) += char
        }
        
      }
      val categorySets = categoryBuilder.mapValues(ranges => CharSet(RangeOps.union(ranges.to[Seq]))).toMap
      val propertySets = propertyBuilder.mapValues(ranges => CharSet(RangeOps.union(ranges.to[Seq]))).toMap
      (categorySets, propertySets)
    }
    logger.debug(s"Initialized Unicode general category and binary property catalog in $elapsed")
    ret
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
    "Space" -> space)

}

