package dregex

import org.scalatest.FunSuite

class CompileTest extends FunSuite {

  test("compilation") {
    Regex.compile("a" * 2000) // long juxtaposition  
    Regex.compile("a{2000}") // long repetition
  }
  
}