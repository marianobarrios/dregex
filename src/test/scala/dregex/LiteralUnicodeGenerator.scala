package dregex

import dregex.impl.RegexTree.{AbstractRange, CharRange, CharSet}
import dregex.impl.{UnicodeChar, Util}

import java.lang.Character.{UnicodeBlock, UnicodeScript}
import scala.collection.JavaConverters.mapAsScalaMapConverter
import dregex.impl.UnicodeChar.FromIntConversion

import scala.collection.immutable.SortedMap

// [CROSS-BUILD] For immutable collections in Scala < 2.13
import scala.collection.immutable.Seq

// [CROSS-BUILD] For mapValues in Scala < 2.13
import scala.collection.compat._

object LiteralUnicodeGenerator {

  private val blockRanges: Seq[(UnicodeBlock, (Int, Int))] = {
    val blockStarts = Util.getPrivateStaticField[Array[Int]](classOf[UnicodeBlock], "blockStarts")
    val javaBlocks = Util.getPrivateStaticField[Array[UnicodeBlock]](classOf[UnicodeBlock], "blocks").toSeq
    blockStarts.indices.flatMap { i =>
      val from = blockStarts(i)
      val to =
        if (i == blockStarts.length - 1)
          UnicodeChar.max.codePoint
        else
          blockStarts(i + 1) - 1
      // skip unassigned blocks
      javaBlocks.map(Option(_)).apply(i).map { block =>
        (block, (from, to))
      }
    }
  }

  private val blockAliases: Map[UnicodeBlock, Seq[String]] = {
    val alias =
      Util.getPrivateStaticField[java.util.Map[String, UnicodeBlock]](classOf[UnicodeBlock], "map").asScala.toMap
    alias
      .groupBy { case (_, v) => v }
      // [CROSS-BUILD] For immutable collections in Scala < 2.13
      .view
      .mapValues(v => v.keys.toIndexedSeq)
      .toMap
  }

  private val scriptRanges: Seq[(UnicodeScript, CharSet)] = {
    val scriptStarts = Util.getPrivateStaticField[Array[Int]](classOf[UnicodeScript], "scriptStarts")
    val javaScripts = Util.getPrivateStaticField[Array[UnicodeScript]](classOf[UnicodeScript], "scripts").toSeq
    // [CROSS-BUILD] Using immutable SortedMap as the immutable one was introduced in Scala 2.12
    var builder = SortedMap[UnicodeScript, CharSet]()
    for (i <- scriptStarts.indices) {
      val from = scriptStarts(i)
      val to =
        if (i == scriptStarts.length - 1)
          UnicodeChar.max.codePoint
        else
          scriptStarts(i + 1) - 1
      // skip unknown (unassigned) scripts
      if (javaScripts(i) != UnicodeScript.UNKNOWN) {
        val CharSet(existing) = builder.getOrElse(javaScripts(i), CharSet(Seq()))
        builder += javaScripts(i) -> CharSet(existing :+ CharRange(from.u, to.u))
      }
    }
    // [CROSS-BUILD] Ask for IndexedSeq to force immutable.Seq
    builder.toIndexedSeq
  }

  private val scriptAliases: Map[UnicodeScript, Seq[String]] = {
    val alias =
      Util.getPrivateStaticField[java.util.Map[String, UnicodeScript]](classOf[UnicodeScript], "aliases").asScala.toMap
    alias
      .groupBy { case (_, v) => v }
      // [CROSS-BUILD] For immutable collections in Scala < 2.13
      .view
      .mapValues(v => v.keys.toIndexedSeq)
      .toMap
  }

  private def toHex(codePoint: Int): String = {
    String.format("0x%04X", Int.box(codePoint))
  }

  private def toHexPair(range: AbstractRange): String = {
    s"(${toHex(range.from.codePoint)}, ${toHex(range.to.codePoint)})"
  }

  private def toHexPairs(set: CharSet): String = {
    set.ranges.map(toHexPair(_)).mkString(", ")
  }

  def main(args: Array[String]): Unit = {
    println("val blocksRanges: Map[Seq[String], (Int, Int)] = Map(")
    for (((block, (from, to)), i) <- blockRanges.zipWithIndex) yield {
      val blocksAndAliases = blockAliases(block)
      print(
        s"  Seq(${blocksAndAliases.map(name => '"' + name + '"').mkString(", ")}) -> (${toHex(from)}, ${toHex(to)})")
      if (i < blockRanges.size - 1) {
        print(",")
      }
      println()
    }

    println(")")
    println()
    println("val scriptRanges: Map[Seq[String], Seq[(Int, Int)]] = Map(")
    for (((script, charSet), i) <- scriptRanges.zipWithIndex) yield {
      val scriptsAndAliases = script.toString +: scriptAliases(script)
      print(s"  Seq(${scriptsAndAliases.map(name => '"' + name + '"').mkString(", ")}) -> Seq(${toHexPairs(charSet)})")
      if (i < scriptRanges.size - 1) {
        print(",")
      }
      println()
    }
    println(")")
  }
}