package dregex

import java.util.regex.Pattern

import org.scalatest.funsuite.AnyFunSuite

class OperationsTest extends AnyFunSuite {

  private def doIntersect(left: String, right: String): Boolean = {
    val compiled = Regex.compile(java.util.List.of(left, right), Pattern.DOTALL)
    compiled.get(0) doIntersect compiled.get(1)
  }

  private def isSubset(left: String, right: String): Boolean = {
    val compiled = Regex.compile(java.util.List.of(left, right), Pattern.DOTALL)
    compiled.get(0) isSubsetOf compiled.get(1)
  }

  private def isProperSubset(left: String, right: String): Boolean = {
    val compiled = Regex.compile(java.util.List.of(left, right), Pattern.DOTALL)
    compiled.get(0) isProperSubsetOf compiled.get(1)
  }

  private def testIntersection(left: String, right: String)(result: String): Boolean = {
    val compiled = Regex.compile(java.util.List.of(left, right, result), Pattern.DOTALL)
    (compiled.get(0) intersect compiled.get(1)) equiv compiled.get(2)
  }

  private def testUnion(left: String, right: String)(result: String): Boolean = {
    val compiled = Regex.compile(java.util.List.of(left, right, result), Pattern.DOTALL)
    (compiled.get(0) union compiled.get(1)) equiv compiled.get(2)
  }

  test("intersections - boolean") {
    assertResult(true)(doIntersect("a", "."))
    assertResult(false)(doIntersect("a", "b"))
    assertResult(false)(doIntersect("[^a]", "a"))
    assertResult(false)(doIntersect("[^a]", "[a]"))
    assertResult(false)(doIntersect("[^ab]", "[ab]"))
    assertResult(false)(doIntersect("[^ab]", "a|b"))
    assertResult(false)(doIntersect(".+", ""))
    assertResult(false)(doIntersect("(?!a).", "a"))
  }

  test("subset - boolean") {
    assertResult(true)(isSubset("a", "."))
    assertResult(true)(isSubset("", ".*"))
    assertResult(true)(isSubset("a", "a"))
    assertResult(true)(isSubset("(a|b){2}", "[ab][ab]"))
    assertResult(false)(isSubset("[^a]", "[a]"))
    assertResult(false)(isSubset("[abc]", "[ab]"))
    assertResult(false)(isSubset("[^ab]", "a|b"))
  }

  test("proper subset - boolean") {
    assertResult(true)(isProperSubset("a", "."))
    assertResult(true)(isProperSubset("", ".*"))
    assertResult(true)(isProperSubset("[ab]+", "[ab]*"))
    assertResult(true)(isProperSubset("[ab]", "[abcd]"))
    assertResult(false)(isProperSubset("a", "a"))
    assertResult(false)(isProperSubset("(a|b){2}", "[ab][ab]"))
    assertResult(false)(isProperSubset("[^a]", "[a]"))
    assertResult(false)(isProperSubset("[abc]", "[ab]"))
    assertResult(false)(isProperSubset("[^ab]", "a|b"))
  }

  test("intersections") {
    assert(testIntersection("a", ".")("a"))
    assert(testIntersection("a", "b")("(?!a)a"))
    assert(testIntersection("[^a]", "a")("(?!a)a"))
    assert(testIntersection("[^a]", "[a]")("(?!a)a"))
    assert(testIntersection("[^ab]", "[ab]")("(?!a)a"))
    assert(testIntersection("[^ab]", "a|b")("(?!a)a"))
    assert(testIntersection(".+", "")("(?!a)a"))
  }

  test("union") {
    assert(testUnion("a", "a")("a"))
    assert(testUnion("a", "b")("a|b"))
    assert(testUnion("a", "[^a]")("."))
    assert(testUnion("", ".")(".?"))
    assert(testUnion("a", "a")("a"))
    assert(testUnion("(?!a).", "a")("."))
  }

}
