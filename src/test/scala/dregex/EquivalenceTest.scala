package dregex

import org.scalatest.FunSuite

class EquivalenceTest extends FunSuite {

  private def equiv(left: String, right: String): Boolean = {
    val Seq(leftCompiled, rightCompiled) = Regex.compile(Seq(left, right)).unzip._2
    leftCompiled equiv rightCompiled
  }

  test("grouping") {
    assertResult(false)(equiv("abd", "."))
    assertResult(true)(equiv("(a(b(c)d)e)", "abcde"))
    assertResult(true)(equiv("ab(c)", "(a(b)c)"))
    assertResult(true)(equiv("abc", "(abc)"))
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
    
  test("shortcut characted classes") {
    assertResult(true)(equiv("""\d""", "[0-9]"))
    assertResult(true)(equiv("""\w""", "[a-zA-Z0-9_]"))
    assertResult(true)(equiv("""\s""", """[ \t\n\r\f]"""))
  }
  
  test("disjunctions") {
    val Seq(a, b, c) = Regex.compile(Seq("a|b", "a", "b")).unzip._2
    assertResult(true)(a equiv (b union c))
  }
  
}