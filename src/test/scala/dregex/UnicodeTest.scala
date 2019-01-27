package dregex

import org.scalatest.FunSuite
import TestUtil.using

class UnicodeTest extends FunSuite {

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
