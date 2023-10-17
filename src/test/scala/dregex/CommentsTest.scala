package dregex

import dregex.impl.RegexParser
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommentsTest {

  @Test
  def testCommentsWithFlag() = {
    def equiv(withComments: String, withoutComments: String): Boolean = {
      val normal = RegexParser.parse(withoutComments, new RegexParser.Flags())

      val flags = new RegexParser.Flags()
      flags.comments = true
      val comments = RegexParser.parse(withComments, flags)

      val universe = new Universe(java.util.List.of(normal.getTree, comments.getTree), normal.getNorm)
      val cNormal = new CompiledRegex(withoutComments, normal.getTree, universe)
      val cComments = new CompiledRegex(withComments, comments.getTree, universe)
      cNormal equiv cComments
    }
    assertTrue(equiv("a b # comment", "ab"))
    assertTrue(equiv("a b # comment\nc ", "abc"))
    assertTrue(equiv(" a | b c  ", "a|bc"))
    assertTrue(equiv(" ( a |  b ) c ", "(a|b)c"))
    assertTrue(equiv(" a  b + c ", "ab+c"))
    assertTrue(equiv(" a\\ b", "a b"))
  }

  @Test
  def testCommentsWithEmbeddedFlag() = {
    def equiv(a: String, b: String): Boolean = {
      val c = Regex.compile(java.util.List.of(a, b))
      c.get(0) equiv c.get(1)
    }
    assertTrue(equiv("(?x) a  b c", "abc"))
    assertTrue(equiv("(?x) a  b c", "abc"))
  }

}
