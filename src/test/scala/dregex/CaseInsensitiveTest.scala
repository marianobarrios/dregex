package dregex

import java.util.regex.Pattern

import dregex.TestUtil.using
import org.scalatest.FunSuite
import org.scalatest.Matchers

class CaseInsensitiveTest extends FunSuite with Matchers {

  test("case insensitive") {

    using(Regex.compile("a", Pattern.CASE_INSENSITIVE)) { r =>
      assertResult(true)(r.matches("A"))
      assertResult(true)(r.matches("a"))
    }

    using(Regex.compile("á", Pattern.CASE_INSENSITIVE)) { r =>
      assertResult(false)(r.matches("Á"))
      assertResult(true)(r.matches("á"))
    }

    using(Regex.compile("á", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS)) { r =>
      assertResult(true)(r.matches("Á"))
      assertResult(true)(r.matches("á"))
    }

    using(Regex.compile("á", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)) { r =>
      assertResult(true)(r.matches("Á"))
      assertResult(true)(r.matches("á"))
    }

  }

}
