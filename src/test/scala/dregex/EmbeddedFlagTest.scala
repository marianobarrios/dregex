package dregex

import org.scalatest.FunSuite
import org.scalatest.Matchers

class EmbeddedFlagTest extends FunSuite with Matchers {

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
