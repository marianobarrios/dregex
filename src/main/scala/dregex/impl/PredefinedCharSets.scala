package dregex.impl

import dregex.impl.RegexTree.AbstractRange
import dregex.impl.RegexTree.Lit
import dregex.impl.RegexTree.Wildcard
import dregex.impl.UnicodeChar.FromCharConversion
import dregex.impl.UnicodeChar.FromIntConversion
import dregex.impl.RegexTree.CharSet
import dregex.impl.RegexTree.CharRange
import java.lang.Character.UnicodeBlock
import scala.collection.JavaConversions._
import scala.collection.breakOut

object PredefinedCharSets {

  private def getPrivateStaticField[A](clazz: Class[_], name: String): A = {
    val field = clazz.getDeclaredField(name)
    field.setAccessible(true)
    field.get(null).asInstanceOf[A]
  }

  val unicodeBlocks: Map[String, CharSet] = {
    val blockStarts = getPrivateStaticField[Array[Int]](classOf[UnicodeBlock], "blockStarts")
    val javaBlocks = getPrivateStaticField[Array[UnicodeBlock]](classOf[UnicodeBlock], "blocks").toSeq
    val blockToSetMap: Map[UnicodeBlock, CharSet] = (0 until blockStarts.length).flatMap { i =>
      val from = blockStarts(i)
      val to = if (i == blockStarts.length - 1)
        UnicodeChar.max.codePoint
      else
        blockStarts(i + 1)
      // skip unassigned blocks
      javaBlocks.lift(i).map { block =>
        block -> CharSet.fromRange(CharRange(from.u, to.u))
      }
    } (breakOut)
    val alias = getPrivateStaticField[java.util.Map[String, UnicodeBlock]](classOf[UnicodeBlock], "map").toMap
    alias.mapValues { javaUnicodeBlock =>
      blockToSetMap.get(javaUnicodeBlock).getOrElse {
        /*
         * As of Java 1.8, there exists one deprecated block (UnicodeBlock.SURROGATES_AREA) 
         * that doesn't have any range assigned. Respect Java behavior and make it match nothing. 
         */
        CharSet(Seq())
      }
    }
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

