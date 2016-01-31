package dregex

import org.scalatest.FunSuite

class OperationsTest extends FunSuite {

  private def doIntersect(left: String, right: String): Boolean = {
    val Seq(leftCompiled, rightCompiled) = Regex.compile(Seq(left, right)).unzip._2
    leftCompiled doIntersect rightCompiled
  }

  private def isSubset(left: String, right: String): Boolean = {
    val Seq(leftCompiled, rightCompiled) = Regex.compile(Seq(left, right)).unzip._2
    leftCompiled isSubsetOf rightCompiled
  }

  private def isProperSubset(left: String, right: String): Boolean = {
    val Seq(leftCompiled, rightCompiled) = Regex.compile(Seq(left, right)).unzip._2
    leftCompiled isProperSubsetOf rightCompiled
  }
  
  private def testIntersection(left: String, right: String)(result: String): Boolean = {
    val Seq(leftCompiled, rightCompiled, resultCompiled) = Regex.compile(Seq(left, right, result)).unzip._2
    (leftCompiled intersect rightCompiled) equiv resultCompiled
  } 
  
  private def testUnion(left: String, right: String)(result: String): Boolean = {
    val Seq(leftCompiled, rightCompiled, resultCompiled) = Regex.compile(Seq(left, right, result)).unzip._2
    (leftCompiled union rightCompiled) equiv resultCompiled
  }
  
  test("intersections - boolean") {
    assertResult(true)(doIntersect("a", "."))
    assertResult(false)(doIntersect("a", "b"))
    assertResult(false)(doIntersect("[^a]", "a"))
    assertResult(false)(doIntersect("[^a]", "[a]"))
    assertResult(false)(doIntersect("[^ab]", "[ab]"))
    assertResult(false)(doIntersect("[^ab]", "a|b"))
    assertResult(false)(doIntersect(".+", ""))
  }

  test("subset - boolean") {
    assertResult(true) (isSubset("a", "."))
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
  }

}
