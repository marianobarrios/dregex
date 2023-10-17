package dregex

import TestUtil.using
import dregex.impl.database.{UnicodeBlocks, UnicodeScripts}
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

import java.lang.Character.{UnicodeBlock, UnicodeScript}
import java.util.regex.Pattern
import scala.util.control.Breaks._
import scala.jdk.CollectionConverters._

class UnicodeTest {

  private[this] val logger = LoggerFactory.getLogger(classOf[UnicodeTest])

  @Test
  def testAstralPlanes() = {
    using(Regex.compile(".")) { r =>
      assertTrue(r.matches("a"))
      assertTrue(r.matches("𐐷"))
      assertTrue(r.matches("\uD801\uDC37"))
    }
    using(Regex.compile("𐐷")) { r =>
      assertFalse(r.matches("a"))
      assertTrue(r.matches("𐐷"))
      assertTrue(r.matches("\uD801\uDC37"))
    }
    using(Regex.compile("𐐷", Pattern.LITERAL)) { r =>
      assertFalse(r.matches("a"))
      assertTrue(r.matches("𐐷"))
      assertTrue(r.matches("\uD801\uDC37"))
    }
  }

  @Test
  def testEscapes() = {

    /*
     * Note that Unicode escaping still happens at the source code level even inside triple quotes, so
     * have to double escape in those cases.
     */

    using(Regex.compile("""\x41""")) { r =>
      assertTrue(r.matches("A"))
    }
    using(Regex.compile("\\u0041")) { r =>
      assertTrue(r.matches("A"))
    }
    using(Regex.compile("""\x{41}""")) { r =>
      assertTrue(r.matches("A"))
    }
    using(Regex.compile("""\x{10437}""")) { r =>
      assertTrue(r.matches("𐐷"))
    }

    // double Unicode escaping
    using(Regex.compile("\\uD801\\uDC37")) { r =>
      assertTrue(r.matches("𐐷"))
    }

    // high surrogate alone, works like a normal character
    using(Regex.compile("\\uD801")) { r =>
      assertFalse(r.matches("A"))
      assertTrue(r.matches("\uD801"))
    }

    // high surrogate followed by normal char, works like two normal characters
    using(Regex.compile("\\uD801\\u0041")) { r =>
      assertFalse(r.matches("A"))
      assertTrue(r.matches("\uD801\u0041"))
      assertTrue(r.matches("\uD801" + "\u0041"))
    }

  }

  @Test
  def testBlocks() = {

    using(Regex.compile("""\p{InGreek}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("Ω"))
      assertFalse(r.matches("z"))
    }

    using(Regex.compile("""\p{InGREEK}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{InGreek and Coptic}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{block=Greek}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{blk=Greek}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
    }

    /*
     * Exhaustively test all combinations of Unicode blocks and code points against
     * the java.util.regex implementation.
     */
    for (block <- UnicodeBlocks.charSets.keySet().asScala.toSeq.sorted) {
      val blockExistsInJava = try {
        Character.UnicodeBlock.forName(block); true
      } catch {
        case _: IllegalArgumentException => false
      }
      if (blockExistsInJava) {
        logger.debug("testing Unicode block {}...", block)
        // a regex that matches any character of the block
        val regexString = f"\\p{block=$block}"
        val regex = Regex.compile(regexString)
        val javaRegex = java.util.regex.Pattern.compile(regexString)
        for (codePoint <- Character.MIN_CODE_POINT to Character.MAX_CODE_POINT) {
          breakable {

            codePoint match {
              // Unicode 14 removed mistakenly added characters at the end of the "Tangut Supplement" block
              // Excluding them for testing in old JVM versions, that can have the old range.
              // Source: https://www.unicode.org/versions/Unicode14.0.0/erratafixed.html
              case x if x >= 0x18D80 && x <= 0x18D8F => break()
              case _ =>
            }

            val codePointAsString = new String(Array(codePoint), 0, 1)
            if (javaRegex.matcher(codePointAsString).matches()) {
              assert(regex.matches(codePointAsString),
                s"- block: $block; java block: ${UnicodeBlock.of(codePoint)}; code point: ${String.format("0x%04X", Int.box(codePoint))}")
            }

          }
        }
      } else {
        logger.debug("skipping Unicode block {} as it's not present in the current Java version", block)
      }
    }

  }

  @Test
  def testScripts() = {

    using(Regex.compile("""\p{IsGreek}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("Ω"))
      assertFalse(r.matches("z"))
    }

    using(Regex.compile("""\p{IsGREEK}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{IsGREEK}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{script=GREK}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{sc=Greek}""")) { r =>
      assertTrue(r.matches("α"))
      assertFalse(r.matches("a"))
    }

    /*
     * Exhaustively test all combinations of Unicode scripts and code points against
     * the java.util.regex implementation.
     */
    for (script <- UnicodeScripts.chatSets.asScala.keys.toSeq.sorted) {
      val scriptExistsInJava = try {
        Character.UnicodeScript.forName(script); true
      } catch {
        case _: IllegalArgumentException => false
      }
      if (scriptExistsInJava) {
        logger.debug("testing Unicode script {}...", script)
        // a regex that matches any character of the block
        val regexString = f"\\p{script=$script}"
        val regex = Regex.compile(regexString)
        val javaRegex = java.util.regex.Pattern.compile(regexString)
        for (codePoint <- Character.MIN_CODE_POINT to Character.MAX_CODE_POINT) {
          breakable {
            // A few code points were removed from scripts as Java versions evolved, ignore them
            codePoint match {

              // these 4 characters were moved from Devanagari to Inherited script,
              // so we have to exclude them from the tests for them to work across different Java versions.
              // Source: https://unicode.org/mail-arch/unicode-ml/y2002-m12/0053.html
              case x if x >= 0x0951 && x <= 0x0954 => break()

              // this character was apparently moved from Javanese to Common script,
              // so we have to exclude them from the tests for them to work across different Java versions.
              case 0xA9CF => break()

              // this character was apparently moved from Kali to Common script,
              // so we have to exclude them from the tests for them to work across different Java versions.
              case 0xA92E => break()

              case _ =>
            }

            // As code points are added to scripts, this test count fail, so we not assert when the
            // code points are not assigned in Java (it can just be an old version).
            val javaScript = UnicodeScript.of(codePoint)
            if (javaScript == UnicodeScript.UNKNOWN || javaScript == UnicodeScript.COMMON) {
              break()
            }

            val codePointAsString = new String(Array(codePoint), 0, 1)
            if (javaRegex.matcher(codePointAsString).matches()) {
              assert(regex.matches(codePointAsString),
                s"- script: $script; java script: ${UnicodeScript.of(codePoint)}; code point: ${String.format("0x%04X", Int.box(codePoint))}")
            }
          }
        }
      } else {
        logger.debug("skipping Unicode script {} as it's not present in the current Java version", script)
      }
    }

  }

  @Test
  def testGeneralCategories() = {

    using(Regex.compile("""\p{Lu}""")) { r =>
      assertTrue(r.matches("A"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{IsLu}""")) { r =>
      assertTrue(r.matches("A"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{general_category=Lu}""")) { r =>
      assertTrue(r.matches("A"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{gc=Lu}""")) { r =>
      assertTrue(r.matches("A"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\p{general_category=L}""")) { r =>
      assertTrue(r.matches("A"))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("-"))
    }

  }

  @Test
  def testBinaryProperties() = {

    using(Regex.compile("""\p{IsAlphabetic}""")) { r =>
      assertTrue(r.matches("A"))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("*"))
    }

    using(Regex.compile("""\p{IsHex_Digit}""")) { r =>
      assertTrue(r.matches("f"))
      assertFalse(r.matches("g"))
    }

  }

  @Test
  def testLinebreak() = {

    using(Regex.compile("""\R""")) { r =>
      assertTrue(r.matches("\n"))
      assertTrue(r.matches("\u000A"))
      assertTrue(r.matches("\u2029"))
      assertFalse(r.matches("\u000A\u000D"))
      assertTrue(r.matches("\u000D\u000A"))
    }

  }

  @Test
  def testJavaCategories() = {

    using(Regex.compile("""\p{javaLowerCase}""")) { r =>
      assertTrue(r.matches("a"))
      assertFalse(r.matches("A"))
    }

  }

}
