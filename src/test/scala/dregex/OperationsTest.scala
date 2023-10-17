package dregex

import org.junit.jupiter.api.Assertions.{assertFalse, assertTrue}
import org.junit.jupiter.api.Test

import java.util.regex.Pattern

class OperationsTest {

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

  private def compareIntersection(left: String, right: String)(result: String): Boolean = {
    val compiled = Regex.compile(java.util.List.of(left, right, result), Pattern.DOTALL)
    (compiled.get(0) intersect compiled.get(1)) equiv compiled.get(2)
  }

  private def compareUnion(left: String, right: String)(result: String): Boolean = {
    val compiled = Regex.compile(java.util.List.of(left, right, result), Pattern.DOTALL)
    (compiled.get(0) union compiled.get(1)) equiv compiled.get(2)
  }

  @Test
  def testIntersectionsBoolean() = {
    assertTrue(doIntersect("a", "."))
    assertFalse(doIntersect("a", "b"))
    assertFalse(doIntersect("[^a]", "a"))
    assertFalse(doIntersect("[^a]", "[a]"))
    assertFalse(doIntersect("[^ab]", "[ab]"))
    assertFalse(doIntersect("[^ab]", "a|b"))
    assertFalse(doIntersect(".+", ""))
    assertFalse(doIntersect("(?!a).", "a"))
  }

  @Test
  def testSubsetBoolean() = {
    assertTrue(isSubset("a", "."))
    assertTrue(isSubset("", ".*"))
    assertTrue(isSubset("a", "a"))
    assertTrue(isSubset("(a|b){2}", "[ab][ab]"))
    assertFalse(isSubset("[^a]", "[a]"))
    assertFalse(isSubset("[abc]", "[ab]"))
    assertFalse(isSubset("[^ab]", "a|b"))
  }

  @Test
  def testProperSubsetBoolean() = {
    assertTrue(isProperSubset("a", "."))
    assertTrue(isProperSubset("", ".*"))
    assertTrue(isProperSubset("[ab]+", "[ab]*"))
    assertTrue(isProperSubset("[ab]", "[abcd]"))
    assertFalse(isProperSubset("a", "a"))
    assertFalse(isProperSubset("(a|b){2}", "[ab][ab]"))
    assertFalse(isProperSubset("[^a]", "[a]"))
    assertFalse(isProperSubset("[abc]", "[ab]"))
    assertFalse(isProperSubset("[^ab]", "a|b"))
  }

  @Test
  def testIintersections() = {
    assertTrue(compareIntersection("a", ".")("a"))
    assertTrue(compareIntersection("a", "b")("(?!a)a"))
    assertTrue(compareIntersection("[^a]", "a")("(?!a)a"))
    assertTrue(compareIntersection("[^a]", "[a]")("(?!a)a"))
    assertTrue(compareIntersection("[^ab]", "[ab]")("(?!a)a"))
    assertTrue(compareIntersection("[^ab]", "a|b")("(?!a)a"))
    assertTrue(compareIntersection(".+", "")("(?!a)a"))
  }

  @Test
  def testUnion() = {
    assertTrue(compareUnion("a", "a")("a"))
    assertTrue(compareUnion("a", "b")("a|b"))
    assertTrue(compareUnion("a", "[^a]")("."))
    assertTrue(compareUnion("", ".")(".?"))
    assertTrue(compareUnion("a", "a")("a"))
    assertTrue(compareUnion("(?!a).", "a")("."))
  }

}
