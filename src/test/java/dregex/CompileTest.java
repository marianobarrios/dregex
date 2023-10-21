package dregex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CompileTest {

  @Test
  void testCompilation()  {
    Regex.compile("a".repeat(2500)); // long juxtaposition
    Regex.compile("a{2500}"); // long repetition
    assertThrows(InvalidRegexException.class, () -> Regex.compile("\\1")); // backreferences
  }

}
