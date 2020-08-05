package dregex

import org.scalatest.funsuite.AnyFunSuite

class EmbeddedFlagTest extends AnyFunSuite {

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
