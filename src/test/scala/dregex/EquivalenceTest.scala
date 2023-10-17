package dregex

import java.util.regex.Pattern

import org.scalatest.funsuite.AnyFunSuite

class EquivalenceTest extends AnyFunSuite {

  private def equiv(left: String, right: String): Boolean = {
    val compiled = Regex.compile(java.util.List.of(left, right), Pattern.DOTALL)
    compiled.get(0) equiv compiled.get(1)
  }

  test("grouping") {
    assertResult(false)(equiv("abd", "."))
    assertResult(true)(equiv("(a(b(c)d)e)", "abcde"))
    assertResult(true)(equiv("ab(c)", "(a(b)c)"))
    assertResult(true)(equiv("abc", "(abc)"))
    assertResult(true)(equiv("abc", "(?:abc)"))
    assertThrows[InvalidRegexException] {
      assertResult(true)(equiv("abc", "(?<name>abc)"))
    }
  }

  test("quantifiers") {
    assertResult(true)(equiv("(a|b)+", "(a+|b+)+"))
    assertResult(true)(equiv("a+", "aa*"))
    assertResult(true)(equiv("a*a*", "a*"))
    assertResult(true)(equiv("a?a*", "a*"))
    assertResult(true)(equiv("(ab)+", "ab(ab)*"))
    assertResult(true)(equiv("a", "a{1}"))
    assertResult(true)(equiv("aa", "a{2}"))
    assertResult(true)(equiv("aaa", "a{3}"))
    assertResult(true)(equiv("a{0}", ""))
    assertResult(true)(equiv("a{1}", "a"))
    assertResult(true)(equiv("(a{2}){3}", "a{6}"))
    assertResult(true)(equiv("(a{2}){3}", "a{5}a"))
    assertResult(false)(equiv("(a{2}){3}", "a{5}"))
    assertResult(true)(equiv("a{2,3}", "aaa?"))
    assertResult(true)(equiv("a{2,3}", "a{2}a?"))
    assertResult(true)(equiv("a{0,3}", "a{0,2}a?"))
    assertResult(true)(equiv("a{3,}", "a{3}a*"))
    assertResult(true)(equiv("a{3,}", "a{2}a+"))
    assertResult(true)(equiv("a{3,}", "aaa+"))
  }

  test("lookaround") {
    assertResult(true)(equiv("(?!a|b)(?!c).*", "(?!a|b|c).*"))
    assertResult(true)(equiv("a(?!b)", "a"))
    assertResult(true)(equiv("(?!b).|b", "."))
    assertResult(true)(equiv("a(?!b).|ab", "a."))
    assertResult(true)(equiv("a((?!b).|b)", "a."))
    assertResult(true)(equiv("a((?!b).|b)+", "a.+"))
    assertResult(true)(equiv("(a((?!b).|b))+", "(a.)+"))
    assertResult(true)(equiv("(?!a)(?!b).|c", "(?!a)(?!b)."))
    assertResult(true)(equiv("(?=.).*", ".+"))
    assertResult(true)(equiv("(?!a.*).+", "(?!a).+"))
    assertResult(true)(equiv("(?!a.?).+", "(?!a).+"))
    assertResult(true)(equiv("(?!a.{0,34}).+", "(?!a).+"))
    assertResult(true)(equiv("(?=a.*).+", "(?=a).+"))
    assertResult(true)(equiv("(?=a.?).+", "(?=a).+"))
    assertResult(true)(equiv("(?=a.{0,34}).+", "(?=a).+"))
    assertResult(true)(equiv("(?=.*).+", ".+"))
    assertResult(true)(equiv("(?=.?).+", ".+"))
    assertResult(true)(equiv("(?=.{0,34}).+", ".+"))
    assertResult(true)(equiv("(?=a*).+", ".+"))
    assertResult(true)(equiv("(?=a?).+", ".+"))
    assertResult(true)(equiv("(?=a{0,34}).+", ".+"))
  }

  test("characted classes") {
    assertResult(true)(equiv("[a]", "a"))
    assertResult(true)(equiv("a|b|c", "[abc]"))
    assertResult(true)(equiv("[abcdef]", "[a-f]"))
    assertResult(true)(equiv("[a-cdef]", "[a-f]"))
    assertResult(true)(equiv("[a-cd-f]", "[a-f]"))
  }

  test("combined") {
    assertResult(true)(equiv("(?!a|b).+", "[^ab].*"))
    assertResult(true)(equiv("(?=a|b).*", "[ab].*"))
  }

  test("shortcut character classes") {
    assertResult(true)(equiv("""\d""", "[0-9]"))
    assertResult(true)(equiv("""\w""", "[a-zA-Z0-9_]"))
    assertResult(true)(equiv("""\s""", """[ \t\n\r\f\x{B}]"""))
    assertResult(true)(equiv("""\d""", """[\d]"""))
  }

  test("posix character classes") {
    assertResult(true)(equiv("""\p{Lower}""", "[a-z]"))
    assertResult(true)(equiv("""\p{Upper}""", "[A-Z]"))
    assertResult(true)(equiv("""\p{ASCII}""", """[\x{0}-\x{7F}]"""))
    assertResult(true)(equiv("""\p{Alpha}""", """[\p{Lower}\p{Upper}]"""))
    assertResult(true)(equiv("""\p{Digit}""", "[0-9]"))
    assertResult(true)(equiv("""\p{Alnum}""", """[\p{Alpha}\p{Digit}]"""))
    assertResult(true)(equiv("""\p{Punct}""", """[!"#$%&'()*+,-./:;<=>?@[\\\]\^_`{|}~]"""))
    assertResult(true)(equiv("""\p{Graph}""", """[\p{Alnum}\p{Punct}]"""))
    assertResult(true)(equiv("""\p{Print}""", """[\p{Graph}\x{20}]"""))
    assertResult(true)(equiv("""\p{Blank}""", """[ \t]"""))
    assertResult(true)(equiv("""\p{Cntrl}""", """[\x{0}-\x{1F}\x{7F}]"""))
    assertResult(true)(equiv("""\p{XDigit}""", "[0-9a-fA-F]"))
    assertResult(true)(equiv("""\p{Space}""", """[ \t\n\x0B\f\r]"""))
    assertResult(true)(equiv("""\p{Lower}""", """[\p{Lower}]"""))
  }

  test("disjunctions") {
    val compiled = Regex.compile(java.util.List.of("a|b", "a", "b"))
    assertResult(true)(compiled.get(0) equiv (compiled.get(1) union compiled.get(2)))
  }

  test("block quotes and literal flag") {
    assertResult(true)(equiv("""\Q\E""", ""))
    assertResult(true)(equiv("""\Qabc\E""", "abc"))
    assertResult(true)(equiv("""\Qa*\E""", """a\*"""))
    assertResult(true)(equiv("""(\Qa*\E)*""", """(a\*)*"""))
    assertResult(true)(equiv("""\Q(\E""", """\("""))
    assertResult(true)(equiv("""\Q)\E""", """\)"""))
    assertResult(true)(equiv("""(\Q)a\E)""", """\)a"""))
    assertResult(true)(equiv("""\Q|\E""", """\|"""))
    assertResult(true) {
      val r = Regex.compile("""a|bc""", Pattern.LITERAL)
      r.matches("a|bc")
    }
  }

}
