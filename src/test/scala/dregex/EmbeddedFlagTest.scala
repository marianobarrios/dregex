package dregex

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class EmbeddedFlagTest {

  @Test
  def testEmbeddedFlags() = {
    // OK
    Regex.compile("(?x)a")

    // flags in the middle
    assertThrows(classOf[InvalidRegexException], new Executable {
      def execute() = Regex.compile(" (?x)a")
    })
    assertThrows(classOf[InvalidRegexException], new Executable {
      def execute() = Regex.compile("(?x)a(?x)")
    })

    // unknown flag
    assertThrows(classOf[InvalidRegexException], new Executable {
      def execute() = Regex.compile("(?w)a")
    })
  }

}
