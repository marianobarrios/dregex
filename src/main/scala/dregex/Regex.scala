package dregex

import com.typesafe.scalalogging.slf4j.StrictLogging
import dregex.impl.RegexParser
import dregex.impl.Dfa
import dregex.impl.NormTree
import scala.util.Try
import scala.util.Success

trait Regex {

  def dfa: Dfa
  def universe: Universe

  private def checkUniverse(other: Regex): Unit = {
    if (other.universe != universe)
      throw new Exception("Different universes!")
  }
  
  def matches(string: String): Boolean = {
    val (result, _) = matchesWithPos(string)
    result
  }
  
  def matchesWithPos(string: String): (Boolean, Int) = {
    val genDfa = dfa.dfa
    var current = genDfa.initial
    var i = 0
    for (char <- string) {
      val currentTrans = genDfa.transitions.getOrElse(current, Map())
      val effChar = if (universe.alphabet.contains(char))
        NormTree.Lit(char)
      else
        NormTree.Other
      current = currentTrans.get(effChar) match {
        case Some(newState) => newState
        case None => return (false, i)
      }
      i += 1
    }
    (genDfa.accepting.contains(current), i)
  }

  def intersect(other: Regex): Regex = {
    checkUniverse(other)
    val left = dfa
    val right = other.dfa
    new SynteticRegex(left intersect right, universe)
  }
  
  def diff(other: Regex): Regex = {
    checkUniverse(other)
    val left = dfa
    val right = other.dfa
    new SynteticRegex(left diff right, universe)
  }
  
  def union(other: Regex): Regex = {
    checkUniverse(other)
    val left = dfa
    val right = other.dfa
    new SynteticRegex(left union right, universe)
  }
  
  def doIntersect(other: Regex): Boolean = intersect(other).matchesAnything()

  /**
   * Return whether this regular expression is equivalent to other. Two regular expressions are equivalent if they
   * match exactly the same set of strings.
   */
  def equiv(other: Regex): Boolean = {
    checkUniverse(other)
    !(dfa diff other.dfa).matchesAnything() && !(other.dfa diff dfa).matchesAnything()
  }

  def matchesAnything() = dfa.matchesAnything()

}

object Regex {

  def compile(regex: String): CompiledRegex = {
    val tree = RegexParser.parse(regex)
    new CompiledRegex(tree, new Universe(Seq(tree)))
  }

  def tryCompile(regexs: Seq[String]): Seq[(String, Try[CompiledRegex])] = {
    val results = regexs.map(r => (r, Try(RegexParser.parse(r))))
    val parsedRegexs = results.unzip._2.collect { case Success(r) => r }
    val universe = new Universe(parsedRegexs)
    for ((regex, tree) <- results) yield regex -> tree.map(t => new CompiledRegex(t, universe))
  }
  
  def compile(regexs: Seq[String]): Seq[(String, CompiledRegex)] = {
    val trees = regexs.map(r => (r, RegexParser.parse(r)))
    val universe = new Universe(trees.unzip._2)
    for ((regex, tree) <- trees) yield regex -> new CompiledRegex(tree, universe)
  }
  
  /**
   * Create a Regex that does not match anything. Note that that is different from matching the empty string.
   * Despite the theorical equivalence between automata and regular expressions, in practice there is no regular
   * expression that does not match anything.
   */
  def nullRegex(u: Universe) = new SynteticRegex(Dfa.NothingDfa, u)

}
