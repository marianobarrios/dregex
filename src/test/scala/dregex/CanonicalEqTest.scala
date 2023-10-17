package dregex

import java.util.regex.Pattern
import dregex.TestUtil.using
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.Test

class CanonicalEqTest {

  @Test
  def testCanonicalEquivalence() = {

    using(Regex.compile("\u00F6")) { r =>
      assertTrue(r.matches("\u00F6"))
      assertFalse(r.matches("\u006F\u0308"))
    }

    using(Regex.compile("\u00F6", Pattern.CANON_EQ)) { r =>
      assertTrue(r.matches("\u00F6"))
      assertTrue(r.matches("\u006F\u0308"))
    }

  }

}
