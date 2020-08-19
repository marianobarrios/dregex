package dregex

import dregex.impl.RegexParser
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.immutable.Seq

class CommentsTest extends AnyFunSuite {

  test("comments with flag") {
    def equiv(withComments: String, withoutComments: String): Boolean = {
      val normal = RegexParser.parse(withoutComments)
      val comments = RegexParser.parse(withComments, RegexParser.Flags(comments = true))
      val universe = new Universe(Seq(normal.tree, comments.tree), normal.norm)
      val cNormal = new CompiledRegex(withoutComments, normal.tree, universe)
      val cComments = new CompiledRegex(withComments, comments.tree, universe)
      cNormal equiv cComments
    }
    assert(equiv("a b # comment\nc ", "abc"))
    assert(equiv(" a | b c  ", "a|bc"))
    assert(equiv(" ( a |  b ) c ", "(a|b)c"))
    assert(equiv(" a  b + c ", "ab+c"))
  }

  test("comments with embedded flag") {
    def equiv(a: String, b: String): Boolean = {
      val Seq(cA, cB) = Regex.compile(Seq(a, b))
      cA equiv cB
    }
    assert(equiv("(?x) a  b c", "abc"))
    assert(equiv("(?x) a  b c", "abc"))
  }

}
