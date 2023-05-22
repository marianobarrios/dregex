package dregex

import dregex.impl.RegexParser
import org.scalatest.funsuite.AnyFunSuite

class CommentsTest extends AnyFunSuite {

  test("comments with flag") {
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
    assert(equiv("a b # comment", "ab"))
    assert(equiv("a b # comment\nc ", "abc"))
    assert(equiv(" a | b c  ", "a|bc"))
    assert(equiv(" ( a |  b ) c ", "(a|b)c"))
    assert(equiv(" a  b + c ", "ab+c"))
    assert(equiv(" a\\ b", "a b"))
  }

  test("comments with embedded flag") {
    def equiv(a: String, b: String): Boolean = {
      val c = Regex.compile(java.util.List.of(a, b))
      c.get(0) equiv c.get(1)
    }
    assert(equiv("(?x) a  b c", "abc"))
    assert(equiv("(?x) a  b c", "abc"))
  }

}
