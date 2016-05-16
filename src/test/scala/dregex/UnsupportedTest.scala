package dregex

import org.scalatest.FunSuite

class UnsupportedTest extends FunSuite {

  test("lookbehind") {
    intercept[UnsupportedException](Regex.compile("a(?<!b)c"))
    intercept[UnsupportedException](Regex.compile("a(?<=b)c"))
    intercept[UnsupportedException](Regex.compile("(?<!b)c"))
    intercept[UnsupportedException](Regex.compile("(?<=b)c"))
    intercept[UnsupportedException](Regex.compile("a(?<!b)"))
    intercept[UnsupportedException](Regex.compile("a(?<=b)"))
  }

}