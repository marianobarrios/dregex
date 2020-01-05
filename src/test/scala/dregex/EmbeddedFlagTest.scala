package dregex

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class EmbeddedFlagTest extends AnyFunSuite with Matchers {

  test("embedded flags") {
    // OK
    Regex.compile("(?x)a")

    // flags in the middle
    intercept[InvalidRegexException] {
      Regex.compile(" (?x)a")
    }
    intercept[InvalidRegexException] {
      Regex.compile("(?x)a(?x)")
    }

    // unknown flag
    intercept[InvalidRegexException] {
      Regex.compile("(?w)a")
    }
  }

}
