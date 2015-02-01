package dregex

import dregex.impl.RegexParser
import dregex.impl.Dfa
import dregex.impl.NormTree

trait Regex {

  def dfa: Dfa
  def universe: Universe

  private def checkUniverse(other: Regex): Unit = {
    if (other.universe != universe)
      throw new Exception("cannot make operations between regex from different universes")
  }
  
  def matches(string: String): Boolean = {
    val (result, _) = matchAndReport(string)
    result
  }
  
  def matchAndReport(string: String): (Boolean, Int) = {
    val genDfa = dfa.impl
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
    new SynteticRegex(dfa intersect other.dfa, universe)
  }
  
  def diff(other: Regex): Regex = {
    checkUniverse(other)
    new SynteticRegex(dfa diff other.dfa, universe)
  }
  
  def union(other: Regex): Regex = {
    checkUniverse(other)
    new SynteticRegex(dfa union other.dfa, universe)
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

  def parse(regex: String): ParsedRegex = new ParsedRegex(RegexParser.parse(regex))
  
  def compile(regex: String): CompiledRegex = {
    val tree = parse(regex)
    new CompiledRegex(tree, new Universe(Seq(tree)))
  }

  def compileParsed(tree: ParsedRegex, universe: Universe): CompiledRegex = {
    new CompiledRegex(tree, universe)
  }
  
  def compile(regexs: Seq[String]): Seq[(String, CompiledRegex)] = {
    val trees = regexs.map(r => (r, parse(r)))
    val universe = new Universe(trees.unzip._2)
    for ((regex, tree) <- trees) yield regex -> new CompiledRegex(tree, universe)
  }
  
  /**
   * Create a regular expression that does not match anything. Note that that is different from matching the empty 
   * string. Despite the theoretical equivalence of automata and regular expressions, in practice there is no regular 
   * expression that does not match anything.
   */
  def nullRegex(u: Universe) = new SynteticRegex(Dfa.NothingDfa, u)

}
