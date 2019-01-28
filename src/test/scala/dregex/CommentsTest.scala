package dregex

import dregex.impl.RegexParser
import org.scalatest.FunSuite
import org.scalatest.Matchers

import scala.collection.immutable.Seq

class CommentsTest extends FunSuite with Matchers {

  test("comments with flag") {
    def equiv(withComments: String, withoutComments: String): Boolean = {
      val normal = RegexParser.parse(withoutComments)
      val comments = RegexParser.parse(withComments, comments = true)
      val universe = new Universe(Seq(normal, comments))
      val cNormal = new CompiledRegex(withoutComments, normal, universe)
      val cComments = new CompiledRegex(withComments, comments, universe)
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
