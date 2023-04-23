package dregex

import TestUtil.using
import dregex.impl.database.{UnicodeBlocks, UnicodeScripts}
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.LoggerFactory

import java.lang.Character.{UnicodeBlock, UnicodeScript}
import scala.util.control.Breaks._
import scala.jdk.CollectionConverters._

class UnicodeTest extends AnyFunSuite {

  private[this] val logger = LoggerFactory.getLogger(classOf[UnicodeTest])

  test("astral planes") {
    using(Regex.compile(".")) { r =>
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("𐐷"))
      assertResult(true)(r.matches("\uD801\uDC37"))
    }
    using(Regex.compile("𐐷")) { r =>
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("𐐷"))
      assertResult(true)(r.matches("\uD801\uDC37"))
    }
  }

  test("escapes") {

    /*
     * Note that Unicode escaping still happens at the source code level even inside triple quotes, so
     * have to double escape in those cases.
     */

    using(Regex.compile("""\x41""")) { r =>
      assertResult(true)(r.matches("A"))
    }
    using(Regex.compile("\\u0041")) { r =>
      assertResult(true)(r.matches("A"))
    }
    using(Regex.compile("""\x{41}""")) { r =>
      assertResult(true)(r.matches("A"))
    }
    using(Regex.compile("""\x{10437}""")) { r =>
      assertResult(true)(r.matches("𐐷"))
    }

    // double Unicode escaping
    using(Regex.compile("\\uD801\\uDC37")) { r =>
      assertResult(true)(r.matches("𐐷"))
    }

    // high surrogate alone, works like a normal character
    using(Regex.compile("\\uD801")) { r =>
      assertResult(false)(r.matches("A"))
      assertResult(true)(r.matches("\uD801"))
    }

    // high surrogate followed by normal char, works like two normal characters
    using(Regex.compile("\\uD801\\u0041")) { r =>
      assertResult(false)(r.matches("A"))
      assertResult(true)(r.matches("\uD801\u0041"))
      assertResult(true)(r.matches("\uD801" + "\u0041"))
    }

  }

  test("blocks") {

    using(Regex.compile("""\p{InGreek}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("Ω"))
      assertResult(false)(r.matches("z"))
    }

    using(Regex.compile("""\p{InGREEK}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{InGreek and Coptic}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{block=Greek}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{blk=Greek}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
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

  test("scripts") {

    using(Regex.compile("""\p{IsGreek}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("Ω"))
      assertResult(false)(r.matches("z"))
    }

    using(Regex.compile("""\p{IsGREEK}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{IsGREEK}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{script=GREK}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{sc=Greek}""")) { r =>
      assertResult(true)(r.matches("α"))
      assertResult(false)(r.matches("a"))
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

  test("general categories") {

    using(Regex.compile("""\p{Lu}""")) { r =>
      assertResult(true)(r.matches("A"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{IsLu}""")) { r =>
      assertResult(true)(r.matches("A"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{general_category=Lu}""")) { r =>
      assertResult(true)(r.matches("A"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{gc=Lu}""")) { r =>
      assertResult(true)(r.matches("A"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\p{general_category=L}""")) { r =>
      assertResult(true)(r.matches("A"))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("-"))
    }

  }

  test("binary properties") {

    using(Regex.compile("""\p{IsAlphabetic}""")) { r =>
      assertResult(true)(r.matches("A"))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("*"))
    }

    using(Regex.compile("""\p{IsHex_Digit}""")) { r =>
      assertResult(true)(r.matches("f"))
      assertResult(false)(r.matches("g"))
    }

  }

  test("linebreak") {

    using(Regex.compile("""\R""")) { r =>
      assertResult(true)(r.matches("\n"))
      assertResult(true)(r.matches("\u000A"))
      assertResult(true)(r.matches("\u2029"))
      assertResult(false)(r.matches("\u000A\u000D"))
      assertResult(true)(r.matches("\u000D\u000A"))
    }

  }

  test("java categories") {

    using(Regex.compile("""\p{javaLowerCase}""")) { r =>
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("A"))
    }

  }

}
