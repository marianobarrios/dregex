package dregex

import org.scalatest.FunSuite
import TestUtil.using

class MatchTest extends FunSuite {

  test("character classes - simple") {

    assertResult(false)(Regex.nullRegex(new Universe(Seq())).matchesAnything())

    using(Regex.compile("")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile(" ")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches(" "))
      assertResult(false)(r.matches("  "))
    }

    using(Regex.compile(".")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("aa"))
    }

    using(Regex.compile("[a-d]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("aa"))
      assertResult(false)(r.matches("x"))
    }

    using(Regex.compile("[^a]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("b"))
    }

    using(Regex.compile("[^ab]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(false)(r.matches("b"))
      assertResult(true)(r.matches("c"))
    }

    using(Regex.compile("[ab-c]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("c"))
      assertResult(false)(r.matches("d"))
    }

    using(Regex.compile("[a-bc]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("c"))
      assertResult(false)(r.matches("d"))
    }

    using(Regex.compile("[^ab-c]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(false)(r.matches("b"))
      assertResult(false)(r.matches("c"))
      assertResult(true)(r.matches("d"))
    }

    using(Regex.compile("[^a-bc]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(false)(r.matches("b"))
      assertResult(false)(r.matches("c"))
      assertResult(true)(r.matches("d"))
    }

  }

  test("character classes - special characters inside") {

    //Special characters inside character classes
    using(Regex.compile("[.]"))(r => assertResult(true)(r.matches(".")))
    using(Regex.compile("[(]"))(r => assertResult(true)(r.matches("(")))
    using(Regex.compile("[)]"))(r => assertResult(true)(r.matches(")")))
    using(Regex.compile("[$]"))(r => assertResult(true)(r.matches("$")))
    using(Regex.compile("[[]"))(r => assertResult(true)(r.matches("[")))
    using(Regex.compile("""[\]]"""))(r => assertResult(true)(r.matches("]")))

    //Dash is interpreted literally inside character classes when it is the first or the last element

    using(Regex.compile("[-]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("-"))
      assertResult(false)(r.matches("x"))
    }

    using(Regex.compile("[-a]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("-"))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("x"))
    }

    using(Regex.compile("[a-]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("-"))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("x"))
    }

    using(Regex.compile("[-a-]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("-"))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("x"))
    }

    using(Regex.compile("[^-]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("-"))
      assertResult(true)(r.matches("a"))
    }

    using(Regex.compile("[^-a]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("-"))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("b"))
    }

    using(Regex.compile("[^a-]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("-"))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("b"))
    }

    using(Regex.compile("[^-a-]")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("-"))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("b"))
    }

  }

  test("character classes - shorthand") {

    using(Regex.compile("""\d""")) { r =>
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("0"))
      assertResult(true)(r.matches("9"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\w""")) { r =>
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("0"))
      assertResult(true)(r.matches("9"))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("A"))
      assertResult(true)(r.matches("_"))
      assertResult(false)(r.matches(":"))
    }

    using(Regex.compile("""\s""")) { r =>
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches(" "))
      assertResult(true)(r.matches("\t"))
      assertResult(true)(r.matches("\n"))
      assertResult(true)(r.matches("\r"))
      assertResult(true)(r.matches("\f"))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\D""")) { r =>
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("0"))
      assertResult(false)(r.matches("9"))
      assertResult(true)(r.matches("a"))
    }

    using(Regex.compile("""\W""")) { r =>
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("0"))
      assertResult(false)(r.matches("9"))
      assertResult(false)(r.matches("a"))
      assertResult(false)(r.matches("A"))
      assertResult(false)(r.matches("_"))
      assertResult(true)(r.matches(":"))
    }

    using(Regex.compile("""\S""")) { r =>
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches(" "))
      assertResult(false)(r.matches("\t"))
      assertResult(false)(r.matches("\n"))
      assertResult(false)(r.matches("\r"))
      assertResult(false)(r.matches("\f"))
      assertResult(true)(r.matches("a"))
    }

    {
      val Seq(a1, a2, a3, b1, b2, b3, c) = 
        Regex.compile(Seq("""\S""", """[\S]""", """[^\s]""", """\s""", """[\s]""", """[^\S]""", ".")).unzip._2
      assertResult(true)(a2 equiv a2)
      assertResult(true)(a2 equiv a3)
      assertResult(true)(a3 equiv a1)
      assertResult(true)(b1 equiv b2)
      assertResult(true)(b2 equiv b3)
      assertResult(true)(b3 equiv b1)
      assertResult(false)(a1 doIntersect b1)
      assertResult(true)((a1 union b1) equiv c)
    }

    {
      val Seq(a1, a2, a3, b1, b2, b3, c) = 
        Regex.compile(Seq("""\D""", """[\D]""", """[^\d]""", """\d""", """[\d]""", """[^\D]""", ".")).unzip._2
      assertResult(true)(a2 equiv a2)
      assertResult(true)(a2 equiv a3)
      assertResult(true)(a3 equiv a1)
      assertResult(true)(b1 equiv b2)
      assertResult(true)(b2 equiv b3)
      assertResult(true)(b3 equiv b1)
      assertResult(false)(a1 doIntersect b1)
      assertResult(true)((a1 union b1) equiv c)
    }

    {
      val Seq(a1, a2, a3, b1, b2, b3, c) = 
        Regex.compile(Seq("""\W""", """[\W]""", """[^\w]""", """\w""", """[\w]""", """[^\W]""", ".")).unzip._2
      assertResult(true)(a2 equiv a2)
      assertResult(true)(a2 equiv a3)
      assertResult(true)(a3 equiv a1)
      assertResult(true)(b1 equiv b2)
      assertResult(true)(b2 equiv b3)
      assertResult(true)(b3 equiv b1)
      assertResult(false)(a1 doIntersect b1)
      assertResult(true)((a1 union b1) equiv c)
    }

    {
      val Seq(a, b) = Regex.compile(Seq("""\d""", """[^\D\W]""")).unzip._2
      assertResult(true)(a equiv b)
    }
    
  }

  test("quantifiers") {

    using(Regex.compile("")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("a")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("b"))
      assertResult(false)(r.matches("aa"))
    }

    using(Regex.compile("a*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(false)(r.matches("aab"))
    }

    using(Regex.compile("a+")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(false)(r.matches("aab"))
    }

    using(Regex.compile("a?")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("aa"))
      assertResult(false)(r.matches("aab"))
    }

    using(Regex.compile("(a{2})*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(false)(r.matches("aaa"))
      assertResult(true)(r.matches("aaaa"))
    }

    using(Regex.compile("(a{2})+")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(false)(r.matches("aaa"))
      assertResult(true)(r.matches("aaaa"))
    }

    using(Regex.compile("(a{2,3})*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("aaa"))
      assertResult(true)(r.matches("aaaa"))
      assertResult(true)(r.matches("aaaaa"))
      assertResult(true)(r.matches("aaaaaa"))
      assertResult(true)(r.matches("aaaaaaa"))
    }

    using(Regex.compile("a{0}")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("a{1}")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("aa"))
    }

    using(Regex.compile("a{2}")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(false)(r.matches("aaa"))
    }

    using(Regex.compile("a{1,3}")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("aaa"))
      assertResult(false)(r.matches("aaaa"))
      assertResult(false)(r.matches("aab"))
    }

    using(Regex.compile("a{2,}")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("aaa"))
      assertResult(false)(r.matches("aab"))
    }

    using(Regex.compile("a{0,2}")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(false)(r.matches("aaa"))
      assertResult(false)(r.matches("aab"))
    }

    // watch out!
    using(Regex.compile("a{,2}")) { r =>
      assertResult(true)(r.matches("a{,2}"))
    }

  }

  test("disjunctions") {

    using(Regex.compile("a|b")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("b"))
      assertResult(false)(r.matches("c"))
      assertResult(false)(r.matches("aa"))
    }

    using(Regex.compile("ab|c")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(false)(r.matches("b"))
      assertResult(true)(r.matches("c"))
      assertResult(false)(r.matches("aa"))
      assertResult(true)(r.matches("ab"))
      assertResult(false)(r.matches("abc"))
    }

    using(Regex.compile("(a|b)c")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("ac"))
      assertResult(true)(r.matches("bc"))
      assertResult(false)(r.matches("cc"))
      assertResult(false)(r.matches("aca"))
    }

  }

  test("quantifiers with disjunctions") {

    using(Regex.compile("((a|b)c)+")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("ac"))
      assertResult(true)(r.matches("bc"))
      assertResult(false)(r.matches("acc"))
      assertResult(true)(r.matches("acbc"))
    }

    using(Regex.compile("a*|b*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("aa"))
      assertResult(false)(r.matches("ab"))
      assertResult(false)(r.matches("ba"))
    }

    using(Regex.compile("a?|b*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(false)(r.matches("aa"))
      assertResult(false)(r.matches("ab"))
      assertResult(false)(r.matches("ba"))
    }

    using(Regex.compile("(a*|b*)|c")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("c"))
      assertResult(false)(r.matches("ac"))
      assertResult(false)(r.matches("bc"))
      assertResult(false)(r.matches("abc"))
    }

    using(Regex.compile("(a*|b*)*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("ab"))
      assertResult(false)(r.matches("abc"))
    }

    using(Regex.compile("(a*|b*)*|c")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("c"))
      assertResult(false)(r.matches("ac"))
      assertResult(false)(r.matches("bc"))
      assertResult(false)(r.matches("abc"))
    }

    using(Regex.compile("(a*|b*)+")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("bb"))
      assertResult(true)(r.matches("ab"))
      assertResult(true)(r.matches("bbbab"))
    }

    using(Regex.compile("(a+|b+)+")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("bb"))
      assertResult(true)(r.matches("ab"))
      assertResult(true)(r.matches("bbbab"))
    }

    using(Regex.compile("(a+|b+)*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("bb"))
      assertResult(true)(r.matches("ab"))
      assertResult(true)(r.matches("bbbab"))
    }

    using(Regex.compile("a+|b*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("bb"))
      assertResult(false)(r.matches("ab"))
      assertResult(false)(r.matches("bbbab"))
    }

  }

  test("escaping") {

    using(Regex.compile("""\B"""))(r => assertResult(true)(r.matches("""\""")))
    using(Regex.compile("""\\"""))(r => assertResult(true)(r.matches("""\""")))

    using(Regex.compile("""\U00000041"""))(r => assertResult(true)(r.matches("A")))
    using(Regex.compile("""\u0041"""))(r => assertResult(true)(r.matches("A")))
    using(Regex.compile("""\x41"""))(r => assertResult(true)(r.matches("A")))
    using(Regex.compile("""\x041"""))(r => assertResult(true)(r.matches("A")))
    using(Regex.compile("""\101"""))(r => assertResult(true)(r.matches("A")))

    using(Regex.compile("""\n"""))(r => assertResult(true)(r.matches("\n")))
    using(Regex.compile("""\r"""))(r => assertResult(true)(r.matches("\r")))
    using(Regex.compile("""\t"""))(r => assertResult(true)(r.matches("\t")))
    using(Regex.compile("""\f"""))(r => assertResult(true)(r.matches("\f")))
    using(Regex.compile("""\b"""))(r => assertResult(true)(r.matches("\b")))

    using(Regex.compile("""\.""")) { r =>
      assertResult(true)(r.matches("."))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("""\+"""))(r => assertResult(true)(r.matches("+")))
    using(Regex.compile("""\("""))(r => assertResult(true)(r.matches("(")))
    using(Regex.compile("""\)"""))(r => assertResult(true)(r.matches(")")))

  }

  test("lookahead") {

    using(Regex.compile("(?!b)")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("(?!.?)")) { r =>
      assertResult(false)(r.matchesAnything)
    }

    using(Regex.compile("(?=b)")) { r =>
      assertResult(false)(r.matchesAnything)
    }

    using(Regex.compile("(?=.?)")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("(a|c)(?!b).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches("ad"))
      assertResult(false)(r.matches("ab"))
      assertResult(false)(r.matches("cb"))
    }

    using(Regex.compile("[ac](?!b).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches("ad"))
      assertResult(false)(r.matches("ab"))
      assertResult(false)(r.matches("cb"))
    }

    using(Regex.compile("(d|[ac])(?!b).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches("ad"))
      assertResult(false)(r.matches("db"))
      assertResult(false)(r.matches("ab"))
      assertResult(false)(r.matches("cb"))
    }

    using(Regex.compile("(?!b)a")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("a"))
    }

    using(Regex.compile("a(?!b)")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches("a"))
    }

    using(Regex.compile("(?!a)a")) { r =>
      assertResult(false)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("(?!.*)a")) { r =>
      assertResult(false)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
    }

    using(Regex.compile("(?!a)b")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("b"))
    }

    using(Regex.compile("(?!a).")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("b"))
    }

    using(Regex.compile("(?=b)a")) { r =>
      assertResult(false)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(false)(r.matches("b"))
    }

    using(Regex.compile("a(?=b)")) { r =>
      assertResult(false)(r.matchesAnything)
    }

    using(Regex.compile("(?!b).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(true)(r.matches(""))
      assertResult(true)(r.matches("a"))
      assertResult(false)(r.matches("b"))
      assertResult(true)(r.matches("ab"))
      assertResult(false)(r.matches("bb"))
    }

    using(Regex.compile("(?=b).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("b"))
      assertResult(false)(r.matches("ab"))
      assertResult(true)(r.matches("bb"))
    }

    using(Regex.compile("xxx(?=a|b)(?!c).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("xxxa"))
      assertResult(true)(r.matches("xxxb"))
      assertResult(true)(r.matches("xxxax"))
      assertResult(true)(r.matches("xxxbx"))
      assertResult(false)(r.matches("xxxc"))
      assertResult(false)(r.matches("xxxcx"))
      assertResult(false)(r.matches("xxxxb"))
    }

    using(Regex.compile("xxx(?=a|b).(?!c).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(true)(r.matches("xxxay"))
      assertResult(true)(r.matches("xxxby"))
      assertResult(true)(r.matches("xxxay"))
      assertResult(true)(r.matches("xxxby"))
      assertResult(false)(r.matches("xxxyc"))
      assertResult(false)(r.matches("xxxycx"))
    }

    using(Regex.compile("xxx(?!a|b)(?!c).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("xxxa"))
      assertResult(false)(r.matches("xxxb"))
      assertResult(false)(r.matches("xxxax"))
      assertResult(false)(r.matches("xxxbx"))
      assertResult(false)(r.matches("xxxc"))
      assertResult(false)(r.matches("xxxcx"))
      assertResult(true)(r.matches("xxxxb"))
    }

    using(Regex.compile("xxx(?![ab])(?!c).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("xxxa"))
      assertResult(false)(r.matches("xxxb"))
      assertResult(false)(r.matches("xxxax"))
      assertResult(false)(r.matches("xxxbx"))
      assertResult(false)(r.matches("xxxc"))
      assertResult(false)(r.matches("xxxcx"))
      assertResult(true)(r.matches("xxxxb"))
    }

    using(Regex.compile("xxx(?!a|b)(?=.*)(?!c).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches(""))
      assertResult(false)(r.matches("xxxa"))
      assertResult(false)(r.matches("xxxb"))
      assertResult(false)(r.matches("xxxax"))
      assertResult(false)(r.matches("xxxbx"))
      assertResult(false)(r.matches("xxxc"))
      assertResult(false)(r.matches("xxxcx"))
      assertResult(true)(r.matches("xxxxb"))
    }

    !Regex.compile("(?!.?).*").matchesAnything()
    !Regex.compile("(?!.*).*").matchesAnything()
    !Regex.compile("(?!.{0,10}).*").matchesAnything()
    !Regex.compile("(?!a?).*").matchesAnything()
    !Regex.compile("(?!a*).*").matchesAnything()
    !Regex.compile("(?!a{0,10}).*").matchesAnything()

    using(Regex.compile("(?!a).|c.")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches("a"))
      assertResult(true)(r.matches("b"))
      assertResult(true)(r.matches("c"))
      assertResult(true)(r.matches("cx"))
      assertResult(false)(r.matches("bx"))
    }

    using(Regex.compile("(a|aa)(?!b).+")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches("a"))
      assertResult(false)(r.matches("ab"))
      assertResult(true)(r.matches("ac"))
      assertResult(true)(r.matches("aa"))
      assertResult(true)(r.matches("aac"))
      assertResult(true)(r.matches("aab"))
    }

    using(Regex.compile("(a|aa)(?!b)(c|cc)(?!d).*")) { r =>
      assertResult(true)(r.matchesAnything)
      assertResult(false)(r.matches("a"))
      assertResult(false)(r.matches("ab"))
      assertResult(true)(r.matches("ac"))
      assertResult(false)(r.matches("aa"))
      assertResult(true)(r.matches("aac"))
      assertResult(true)(r.matches("acc"))
      assertResult(true)(r.matches("aacc"))
      assertResult(true)(r.matches("aaccd"))
      assertResult(true)(r.matches("aacce"))
    }

  }

}