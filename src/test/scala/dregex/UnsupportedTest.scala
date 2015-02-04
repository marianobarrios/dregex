package dregex

import org.scalatest.FunSuite

class UnsupportedTest extends FunSuite {

  test("deep lookaround") {
    intercept[UnsupportedException](Regex.compile("(?!a)|b"))
    intercept[UnsupportedException](Regex.compile("(?!a(?!b))"))
    intercept[UnsupportedException](Regex.compile("b((?!a)c)"))
  }

  test("lookbehind") {
    intercept[UnsupportedException](Regex.compile("a(?<!b)c"))
    intercept[UnsupportedException](Regex.compile("a(?<=b)c"))
    intercept[UnsupportedException](Regex.compile("(?<!b)c"))
    intercept[UnsupportedException](Regex.compile("(?<=b)c"))
    intercept[UnsupportedException](Regex.compile("a(?<!b)"))
    intercept[UnsupportedException](Regex.compile("a(?<=b)"))
  }

}