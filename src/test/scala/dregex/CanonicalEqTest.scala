package dregex

import java.util.regex.Pattern

import dregex.TestUtil.using
import org.scalatest.FunSuite
import org.scalatest.Matchers

class CanonicalEqTest extends FunSuite with Matchers {

  test("canonical equivalence") {

    using(Regex.compile("""\u00F6""")) { r =>
      assertResult(true)(r.matches("\u00F6"))
      assertResult(false)(r.matches("\u006F\u0308"))
    }

    using(Regex.compile("""\u00F6""", Pattern.CANON_EQ)) { r =>
      assertResult(true)(r.matches("\u00F6"))
      assertResult(true)(r.matches("\u006F\u0308"))
    }

  }

}
