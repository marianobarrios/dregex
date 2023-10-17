package dregex

import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.Test

import java.util.regex.Pattern

class DotMatchTest {

  @Test
  def testDotMatchModes() = {
    assertFalse(Regex.compile(".+").matches("a\n"))
    assertTrue(Regex.compile(".+", Pattern.DOTALL).matches("a\n"))
    assertFalse(Regex.compile(".+").matches("a\r"))
    assertTrue(Regex.compile(".+", Pattern.UNIX_LINES).matches("a\r"))
  }

  @Test
  def testDotMatchWithFlag() = {
    assertTrue(Regex.compile("(?s).+").matches("a\n"))
    assertTrue(Regex.compile("(?d).+").matches("a\r"))
  }

}
