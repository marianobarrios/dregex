package dregex

import java.util.regex.Pattern
import TestUtil.using
import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.Test

class MatchTest {

  @Test
  def testCharacterClassesSimple() = {

    assertFalse(Regex.nullRegex(Universe.Empty).matchesAtLeastOne())

    using(Regex.compile("")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile(" ")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches(" "))
      assertFalse(r.matches("  "))
    }

    using(Regex.compile(".")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("aa"))
    }

    using(Regex.compile("[a-d]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("aa"))
      assertFalse(r.matches("x"))
    }

    using(Regex.compile("[^a]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("b"))
    }

    using(Regex.compile("[^ab]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertFalse(r.matches("b"))
      assertTrue(r.matches("c"))
    }

    using(Regex.compile("[ab-c]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("c"))
      assertFalse(r.matches("d"))
    }

    using(Regex.compile("[a-bc]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("c"))
      assertFalse(r.matches("d"))
    }

    using(Regex.compile("[^ab-c]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertFalse(r.matches("b"))
      assertFalse(r.matches("c"))
      assertTrue(r.matches("d"))
    }

    using(Regex.compile("[^a-bc]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertFalse(r.matches("b"))
      assertFalse(r.matches("c"))
      assertTrue(r.matches("d"))
    }

  }

  @Test
  def testCharacterClassesSpecialCharactersInside() = {

    // Special characters inside character classes
    using(Regex.compile("[.]"))(r => assertTrue(r.matches(".")))
    using(Regex.compile("[(]"))(r => assertTrue(r.matches("(")))
    using(Regex.compile("[)]"))(r => assertTrue(r.matches(")")))
    using(Regex.compile("[$]"))(r => assertTrue(r.matches("$")))
    using(Regex.compile("[[]"))(r => assertTrue(r.matches("[")))
    using(Regex.compile("""[\]]"""))(r => assertTrue(r.matches("]")))

    // Dash is interpreted literally inside character classes when it is the first or the last element

    using(Regex.compile("[-]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("-"))
      assertFalse(r.matches("x"))
    }

    using(Regex.compile("[-a]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("-"))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("x"))
    }

    using(Regex.compile("[a-]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("-"))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("x"))
    }

    using(Regex.compile("[-a-]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("-"))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("x"))
    }

    using(Regex.compile("[^-]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("-"))
      assertTrue(r.matches("a"))
    }

    using(Regex.compile("[^-a]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("-"))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("b"))
    }

    using(Regex.compile("[^a-]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("-"))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("b"))
    }

    using(Regex.compile("[^-a-]")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("-"))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("b"))
    }

  }

  @Test
  def testCharacterClassesShorthand() = {

    using(Regex.compile("""\d""")) { r =>
      assertFalse(r.matches(""))
      assertTrue(r.matches("0"))
      assertTrue(r.matches("9"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\w""")) { r =>
      assertFalse(r.matches(""))
      assertTrue(r.matches("0"))
      assertTrue(r.matches("9"))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("A"))
      assertTrue(r.matches("_"))
      assertFalse(r.matches(":"))
    }

    using(Regex.compile("""\s""")) { r =>
      assertFalse(r.matches(""))
      assertTrue(r.matches(" "))
      assertTrue(r.matches("\t"))
      assertTrue(r.matches("\n"))
      assertTrue(r.matches("\r"))
      assertTrue(r.matches("\f"))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\D""")) { r =>
      assertFalse(r.matches(""))
      assertFalse(r.matches("0"))
      assertFalse(r.matches("9"))
      assertTrue(r.matches("a"))
    }

    using(Regex.compile("""\W""")) { r =>
      assertFalse(r.matches(""))
      assertFalse(r.matches("0"))
      assertFalse(r.matches("9"))
      assertFalse(r.matches("a"))
      assertFalse(r.matches("A"))
      assertFalse(r.matches("_"))
      assertTrue(r.matches(":"))
    }

    using(Regex.compile("""\S""")) { r =>
      assertFalse(r.matches(""))
      assertFalse(r.matches(" "))
      assertFalse(r.matches("\t"))
      assertFalse(r.matches("\n"))
      assertFalse(r.matches("\r"))
      assertFalse(r.matches("\f"))
      assertTrue(r.matches("a"))
    }

    {
      val compiled =
        Regex.compile(java.util.List.of("""\S""", """[\S]""", """[^\s]""", """\s""", """[\s]""", """[^\S]""", "."), Pattern.DOTALL)
      assertTrue(compiled.get(1) equiv compiled.get(1))
      assertTrue(compiled.get(1) equiv compiled.get(2))
      assertTrue(compiled.get(2) equiv compiled.get(0))
      assertTrue(compiled.get(4) equiv compiled.get(4))
      assertTrue(compiled.get(4) equiv compiled.get(5))
      assertTrue(compiled.get(5) equiv compiled.get(3))
      assertFalse(compiled.get(0) doIntersect compiled.get(4))
      assertTrue((compiled.get(0) union compiled.get(4)) equiv compiled.get(6))
    }

    {
      val compiled =
        Regex.compile(java.util.List.of("""\D""", """[\D]""", """[^\d]""", """\d""", """[\d]""", """[^\D]""", "."), Pattern.DOTALL)
      assertTrue(compiled.get(1) equiv compiled.get(1))
      assertTrue(compiled.get(1) equiv compiled.get(2))
      assertTrue(compiled.get(2) equiv compiled.get(0))
      assertTrue(compiled.get(4) equiv compiled.get(4))
      assertTrue(compiled.get(4) equiv compiled.get(5))
      assertTrue(compiled.get(5) equiv compiled.get(3))
      assertFalse(compiled.get(0) doIntersect compiled.get(4))
      assertTrue((compiled.get(0) union compiled.get(4)) equiv compiled.get(6))
    }

    {
      val compiled =
        Regex.compile(java.util.List.of("""\W""", """[\W]""", """[^\w]""", """\w""", """[\w]""", """[^\W]""", "."), Pattern.DOTALL)
      assertTrue(compiled.get(1) equiv compiled.get(1))
      assertTrue(compiled.get(1) equiv compiled.get(2))
      assertTrue(compiled.get(2) equiv compiled.get(0))
      assertTrue(compiled.get(4) equiv compiled.get(4))
      assertTrue(compiled.get(4) equiv compiled.get(5))
      assertTrue(compiled.get(5) equiv compiled.get(3))
      assertFalse(compiled.get(0) doIntersect compiled.get(4))
      assertTrue((compiled.get(0) union compiled.get(4)) equiv compiled.get(6))
    }

    {
      val compiled = Regex.compile(java.util.List.of("""\d""", """[^\D\W]"""))
      assertTrue(compiled.get(0) equiv compiled.get(1))
    }

  }

  @Test
  def testQuantifiers() = {

    using(Regex.compile("")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("a")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches("a"))
      assertFalse(r.matches("b"))
      assertFalse(r.matches("aa"))
    }

    using(Regex.compile("a*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertFalse(r.matches("aab"))
    }

    using(Regex.compile("a+")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertFalse(r.matches("aab"))
    }

    using(Regex.compile("a?")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("aa"))
      assertFalse(r.matches("aab"))
    }

    using(Regex.compile("(a{2})*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertFalse(r.matches("aaa"))
      assertTrue(r.matches("aaaa"))
    }

    using(Regex.compile("(a{2})+")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertFalse(r.matches("aaa"))
      assertTrue(r.matches("aaaa"))
    }

    using(Regex.compile("(a{2,3})*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("aaa"))
      assertTrue(r.matches("aaaa"))
      assertTrue(r.matches("aaaaa"))
      assertTrue(r.matches("aaaaaa"))
      assertTrue(r.matches("aaaaaaa"))
    }

    using(Regex.compile("a{0}")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("a{1}")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("aa"))
    }

    using(Regex.compile("a{2}")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertFalse(r.matches("aaa"))
    }

    using(Regex.compile("a{1,3}")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("aaa"))
      assertFalse(r.matches("aaaa"))
      assertFalse(r.matches("aab"))
    }

    using(Regex.compile("a{2,}")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("aaa"))
      assertFalse(r.matches("aab"))
    }

    using(Regex.compile("a{0,2}")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertFalse(r.matches("aaa"))
      assertFalse(r.matches("aab"))
    }

    // watch out!
    using(Regex.compile("a{,2}")) { r =>
      assertTrue(r.matches("a{,2}"))
    }

  }

  @Test
  def testDisjunctions() = {

    using(Regex.compile("a|b")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("b"))
      assertFalse(r.matches("c"))
      assertFalse(r.matches("aa"))
    }

    using(Regex.compile("ab|c")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertFalse(r.matches("b"))
      assertTrue(r.matches("c"))
      assertFalse(r.matches("aa"))
      assertTrue(r.matches("ab"))
      assertFalse(r.matches("abc"))
    }

    using(Regex.compile("(a|b)c")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("ac"))
      assertTrue(r.matches("bc"))
      assertFalse(r.matches("cc"))
      assertFalse(r.matches("aca"))
    }

  }

  @Test
  def testQuantifiersWithDisjunctions() = {

    using(Regex.compile("((a|b)c)+")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("ac"))
      assertTrue(r.matches("bc"))
      assertFalse(r.matches("acc"))
      assertTrue(r.matches("acbc"))
    }

    using(Regex.compile("a*|b*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("aa"))
      assertFalse(r.matches("ab"))
      assertFalse(r.matches("ba"))
    }

    using(Regex.compile("a?|b*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("aa"))
      assertFalse(r.matches("ab"))
      assertFalse(r.matches("ba"))
    }

    using(Regex.compile("(a*|b*)|c")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("c"))
      assertFalse(r.matches("ac"))
      assertFalse(r.matches("bc"))
      assertFalse(r.matches("abc"))
    }

    using(Regex.compile("(a*|b*)*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("ab"))
      assertFalse(r.matches("abc"))
    }

    using(Regex.compile("(a*|b*)*|c")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("c"))
      assertFalse(r.matches("ac"))
      assertFalse(r.matches("bc"))
      assertFalse(r.matches("abc"))
    }

    using(Regex.compile("(a*|b*)+")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("bb"))
      assertTrue(r.matches("ab"))
      assertTrue(r.matches("bbbab"))
    }

    using(Regex.compile("(a+|b+)+")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("bb"))
      assertTrue(r.matches("ab"))
      assertTrue(r.matches("bbbab"))
    }

    using(Regex.compile("(a+|b+)*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("bb"))
      assertTrue(r.matches("ab"))
      assertTrue(r.matches("bbbab"))
    }

    using(Regex.compile("a+|b*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("bb"))
      assertFalse(r.matches("ab"))
      assertFalse(r.matches("bbbab"))
    }

  }

  @Test
  def testEscaping() = {

    using(Regex.compile("""\B"""))(r => assertTrue(r.matches("""\""")))
    using(Regex.compile("""\\"""))(r => assertTrue(r.matches("""\""")))

    using(Regex.compile("\u0041"))(r => assertTrue(r.matches("A")))
    using(Regex.compile("""\x41"""))(r => assertTrue(r.matches("A")))
    using(Regex.compile("""\x{41}"""))(r => assertTrue(r.matches("A")))
    using(Regex.compile("""\x{000041}"""))(r => assertTrue(r.matches("A")))
    using(Regex.compile("""\0101"""))(r => assertTrue(r.matches("A")))

    using(Regex.compile("""\n"""))(r => assertTrue(r.matches("\n")))
    using(Regex.compile("""\r"""))(r => assertTrue(r.matches("\r")))
    using(Regex.compile("""\t"""))(r => assertTrue(r.matches("\t")))
    using(Regex.compile("""\f"""))(r => assertTrue(r.matches("\f")))
    using(Regex.compile("""\b"""))(r => assertTrue(r.matches("\b")))

    using(Regex.compile("""\.""")) { r =>
      assertTrue(r.matches("."))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("""\+"""))(r => assertTrue(r.matches("+")))
    using(Regex.compile("""\("""))(r => assertTrue(r.matches("(")))
    using(Regex.compile("""\)"""))(r => assertTrue(r.matches(")")))

  }

  @Test
  def testLookahead() = {

    using(Regex.compile("(?!b)")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("a"))
      assertFalse(r.matches("b"))
    }

    using(Regex.compile("a(?!b).")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("ac"))
      assertFalse(r.matches("ab"))
    }

    using(Regex.compile("a(?!b).|other")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("ac"))
      assertFalse(r.matches("ab"))
      assertTrue(r.matches("other"))
    }

    using(Regex.compile("(a+(?!b).|other)+")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("aaa"))
      assertTrue(r.matches("ac"))
      assertFalse(r.matches("ab"))
      assertFalse(r.matches("aab"))
      assertTrue(r.matches("other"))
      assertTrue(r.matches("otherother"))
      assertTrue(r.matches("aaaother"))
      assertFalse(r.matches("aabother"))
      assertTrue(r.matches("aaotheraa"))
      assertFalse(r.matches("aabotherab"))
    }

    using(Regex.compile("([ax]+(?!b).|other)+")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("x"))
      assertTrue(r.matches("xa"))
      assertTrue(r.matches("xaa"))
      assertTrue(r.matches("xc"))
      assertFalse(r.matches("xb"))
      assertFalse(r.matches("xab"))
      assertTrue(r.matches("other"))
      assertTrue(r.matches("otherother"))
      assertTrue(r.matches("xaaother"))
      assertFalse(r.matches("xabother"))
      assertTrue(r.matches("xaotheraa"))
      assertFalse(r.matches("xabotherab"))
    }

    using(Regex.compile("a+(?!b).")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("ac"))
      assertFalse(r.matches("ab"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("aaa"))
      assertTrue(r.matches("aac"))
      assertFalse(r.matches("aab"))
    }

    using(Regex.compile(".*(?!a).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("aaa"))
    }

    using(Regex.compile("(?!.?)")) { r =>
      assertFalse(r.matchesAtLeastOne())
    }

    using(Regex.compile("(?=b)")) { r =>
      assertFalse(r.matchesAtLeastOne())
    }

    using(Regex.compile("(?=.?)")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("(a|c)(?!b).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches("ad"))
      assertFalse(r.matches("ab"))
      assertFalse(r.matches("cb"))
    }

    using(Regex.compile("[ac](?!b).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches("ad"))
      assertFalse(r.matches("ab"))
      assertFalse(r.matches("cb"))
    }

    using(Regex.compile("(d|[ac])(?!b).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches("ad"))
      assertFalse(r.matches("db"))
      assertFalse(r.matches("ab"))
      assertFalse(r.matches("cb"))
    }

    using(Regex.compile("(?!b)a")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("a"))
    }

    using(Regex.compile("a(?!b)")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches("a"))
    }

    using(Regex.compile("a+(?!b)")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches("a"))
      assertTrue(r.matches("aa"))
    }

    using(Regex.compile("(?!a)a")) { r =>
      assertFalse(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("(?!.*)a")) { r =>
      assertFalse(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
    }

    using(Regex.compile("(?!a)b")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("b"))
    }

    using(Regex.compile("(?!a).")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("b"))
    }

    using(Regex.compile("(?=b)a")) { r =>
      assertFalse(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertFalse(r.matches("b"))
    }

    using(Regex.compile("a(?=b)")) { r =>
      assertFalse(r.matchesAtLeastOne())
    }

    using(Regex.compile("(?!b).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertTrue(r.matches("a"))
      assertFalse(r.matches("b"))
      assertTrue(r.matches("ab"))
      assertFalse(r.matches("bb"))
    }

    using(Regex.compile("(?=b).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("a"))
      assertTrue(r.matches("b"))
      assertFalse(r.matches("ab"))
      assertTrue(r.matches("bb"))
    }

    using(Regex.compile("xxx(?=a|b)(?!c).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("xxxa"))
      assertTrue(r.matches("xxxb"))
      assertTrue(r.matches("xxxax"))
      assertTrue(r.matches("xxxbx"))
      assertFalse(r.matches("xxxc"))
      assertFalse(r.matches("xxxcx"))
      assertFalse(r.matches("xxxxb"))
    }

    using(Regex.compile("xxx(?=a|b).(?!c).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertTrue(r.matches("xxxay"))
      assertTrue(r.matches("xxxby"))
      assertTrue(r.matches("xxxay"))
      assertTrue(r.matches("xxxby"))
      assertFalse(r.matches("xxxyc"))
      assertFalse(r.matches("xxxycx"))
    }

    using(Regex.compile("xxx(?!a|b)(?!c).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("xxxa"))
      assertFalse(r.matches("xxxb"))
      assertFalse(r.matches("xxxax"))
      assertFalse(r.matches("xxxbx"))
      assertFalse(r.matches("xxxc"))
      assertFalse(r.matches("xxxcx"))
      assertTrue(r.matches("xxxxb"))
    }

    using(Regex.compile("xxx(?![ab])(?!c).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("xxxa"))
      assertFalse(r.matches("xxxb"))
      assertFalse(r.matches("xxxax"))
      assertFalse(r.matches("xxxbx"))
      assertFalse(r.matches("xxxc"))
      assertFalse(r.matches("xxxcx"))
      assertTrue(r.matches("xxxxb"))
    }

    using(Regex.compile("xxx(?!a|b)(?=.*)(?!c).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches(""))
      assertFalse(r.matches("xxxa"))
      assertFalse(r.matches("xxxb"))
      assertFalse(r.matches("xxxax"))
      assertFalse(r.matches("xxxbx"))
      assertFalse(r.matches("xxxc"))
      assertFalse(r.matches("xxxcx"))
      assertTrue(r.matches("xxxxb"))
    }

    !Regex.compile("(?!.?).*").matchesAtLeastOne()
    !Regex.compile("(?!.*).*").matchesAtLeastOne()
    !Regex.compile("(?!.{0,10}).*").matchesAtLeastOne()
    !Regex.compile("(?!a?).*").matchesAtLeastOne()
    !Regex.compile("(?!a*).*").matchesAtLeastOne()
    !Regex.compile("(?!a{0,10}).*").matchesAtLeastOne()

    using(Regex.compile("(?!a).|c.")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("a"))
      assertTrue(r.matches("b"))
      assertTrue(r.matches("c"))
      assertTrue(r.matches("cx"))
      assertFalse(r.matches("bx"))
    }

    using(Regex.compile("(a|aa)(?!b).+")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("a"))
      assertFalse(r.matches("ab"))
      assertTrue(r.matches("ac"))
      assertTrue(r.matches("aa"))
      assertTrue(r.matches("aac"))
      assertTrue(r.matches("aab"))
    }

    using(Regex.compile("(a|aa)(?!b)(c|cc)(?!d).*")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("a"))
      assertFalse(r.matches("ab"))
      assertTrue(r.matches("ac"))
      assertFalse(r.matches("aa"))
      assertTrue(r.matches("aac"))
      assertTrue(r.matches("acc"))
      assertTrue(r.matches("aacc"))
      assertTrue(r.matches("aaccd"))
      assertTrue(r.matches("aacce"))
    }

    using(Regex.compile("(?!bb)b")) { r =>
      assertTrue(r.matches("b"))
    }

    using(Regex.compile("((?!bb)b)+")) { r =>
      assertTrue(r.matches("b"))
      // true because lookahead cannot "escape" the expression it's in
      assertTrue(r.matches("bb"))
    }

  }

  @Test
  def testLookbehind() = {

    using(Regex.compile("(?<!b)")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertTrue(r.matches(""))
      assertFalse(r.matches("a"))
      assertFalse(r.matches("b"))
    }

    using(Regex.compile(".(?<!a)b")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("b"))
      assertTrue(r.matches("bb"))
      assertTrue(r.matches("cb"))
      assertFalse(r.matches("ab"))
    }

    using(Regex.compile("other|.(?<!a)b")) { r =>
      assertTrue(r.matchesAtLeastOne())
      assertFalse(r.matches("b"))
      assertTrue(r.matches("bb"))
      assertTrue(r.matches("cb"))
      assertFalse(r.matches("ab"))
      assertTrue(r.matches("other"))
    }

    using(Regex.compile("a(?<!a)")) { r =>
      assertFalse(r.matchesAtLeastOne())
    }

    using(Regex.compile("a(?<!a)|a")) { r =>
      assertTrue(r.matches("a"))
    }

    using(Regex.compile("ong(?<!long)")) { r =>
      assertTrue(r.matches("ong"))
    }

    using(Regex.compile("(a|b)(?<!a|b)")) { r =>
      assertFalse(r.matchesAtLeastOne())
    }

    using(Regex.compile("[a-c](?<!a|b)")) { r =>
      assertTrue(r.matches("c"))
    }

  }

}
