package dregex

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class CompileTest {

  @Test
  def testCompilation() = {
    Regex.compile("a" * 2500) // long juxtaposition
    Regex.compile("a{2500}") // long repetition
    assertThrows(classOf[InvalidRegexException], new Executable {
      def execute() = Regex.compile("""\1""") // backreferences
    })
  }

}
