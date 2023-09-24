package dregex

import org.scalatest.funsuite.AnyFunSuite

class CompileTest extends AnyFunSuite {

  test("compilation") {
    Regex.compile("a" * 2500) // long juxtaposition
    Regex.compile("a{2500}") // long repetition
    intercept[InvalidRegexException] {
      Regex.compile("""\1""") // backreferences
    }
    intercept[InvalidRegexException] {
      Regex.compile("""\b(\w+)(\s+\1)+\b""") // backreference
    }
  }

}
