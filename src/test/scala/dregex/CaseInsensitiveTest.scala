package dregex

import java.util.regex.Pattern
import dregex.TestUtil.using
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.Test

class CaseInsensitiveTest {

  @Test
  def testCaseInsensitive() = {

    using(Regex.compile("a", Pattern.CASE_INSENSITIVE)) { r =>
      assertTrue(r.matches("A"))
      assertTrue(r.matches("a"))
    }

    using(Regex.compile("á", Pattern.CASE_INSENSITIVE)) { r =>
      assertFalse(r.matches("Á"))
      assertTrue(r.matches("á"))
    }

    using(Regex.compile("á", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS)) { r =>
      assertTrue(r.matches("Á"))
      assertTrue(r.matches("á"))
    }

    using(Regex.compile("á", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)) { r =>
      assertTrue(r.matches("Á"))
      assertTrue(r.matches("á"))
    }

  }

}
