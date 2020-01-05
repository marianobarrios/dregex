package dregex

import java.util.regex.Pattern

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class DotMatchTest extends AnyFunSuite with Matchers {

  test("dot match modes") {
    assertResult(false)(Regex.compile(".+").matches("a\n"))
    assertResult(true)(Regex.compile(".+", Pattern.DOTALL).matches("a\n"))
    assertResult(false)(Regex.compile(".+").matches("a\r"))
    assertResult(true)(Regex.compile(".+", Pattern.UNIX_LINES).matches("a\r"))
  }

  test("dot match with flag") {
    assertResult(true)(Regex.compile("(?s).+").matches("a\n"))
    assertResult(true)(Regex.compile("(?d).+").matches("a\r"))
  }

}
