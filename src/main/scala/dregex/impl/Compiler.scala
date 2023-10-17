package dregex.impl

import dregex.InvalidRegexException

import java.util.stream.Collectors
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters._

/**
  * Take a regex AST and produce a NFA.
  * Except when noted the Thompson-McNaughton-Yamada algorithm is used.
  * Reference: http://stackoverflow.com/questions/11819185/steps-to-creating-an-nfa-from-a-regular-expression
  */
class Compiler(intervalMapping: java.util.Map[RegexTree.AbstractRange, java.util.List[CharInterval]]) {

  import RegexTree._

  /**
    * Transform a regular expression abstract syntax tree into a corresponding NFA
    */
  def fromTree(ast: Node): Dfa[SimpleState] = {
    val initial = new SimpleState
    val accepting = new SimpleState
    val transitions = fromTreeImpl(ast, initial, accepting)
    val nfa = new Nfa(initial, transitions.asJava, Set[State](accepting).asJava)
    DfaAlgorithms.rewriteWithSimpleStates(DfaAlgorithms.fromNfa(nfa))
  }

  private def fromTreeImpl(node: Node, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    node match {

      // base case

      case range: AbstractRange =>
        val intervals = intervalMapping.get(range)
        intervals.stream().map(interval => new Nfa.Transition(from, to, interval)).collect(Collectors.toList()).asScala.toSeq

      // recurse

      case CharSet(intervals) =>
        fromTreeImpl(Disj(intervals), from, to)

      case juxt: Juxt =>
        processJuxt(combineNegLookaheads(juxt), from, to)

      case la: Lookaround =>
        fromTreeImpl(Juxt(Seq(la)), from, to)

      case disj: Disj =>
        processDisj(disj, from, to)

      case rep: Rep =>
        processRep(rep, from, to)

      case Intersection(left, right) =>
        processOp(DfaAlgorithms.intersect, left, right, from, to)

      case Union(left, right) =>
        processOp(DfaAlgorithms.union, left, right, from, to)

      case Difference(left, right) =>
        processOp(DfaAlgorithms.diff, left, right, from, to)

      case cg: PositionalCaptureGroup =>
        processCaptureGroup(cg.value, from, to)

      case _: NamedCaptureGroup =>
        throw new InvalidRegexException("named capture groups are not supported")
    }
  }

  /**
    * Lookaround constructions are transformed in equivalent DFA operations, and the result of those trivially transformed
    * into a NFA again for insertion into the outer expression.
    *
    * (?=B)C is transformed into C ∩ B.*
    * (?!B)C is transformed into C - B.*
    * A(?<=B) is transformed into A ∩ .*B
    * A(?<!B) is transformed into A - .*B
    *
    * In the case of more than one lookaround, the transformation is applied recursively.
    *
    * *
    * NOTE: Only lookahead is currently implemented
    */
  private def processJuxt(juxt: Juxt, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    import Direction._
    import Condition._
    findLookaround(juxt.values) match {
      case Some(i) =>
        val prefix = juxt.values.slice(0, i)
        val suffix = juxt.values.slice(i + 1, juxt.values.size)
        val wildcard = Rep(min = 0, max = None, value = Wildcard)
        juxt.values(i).asInstanceOf[Lookaround] match {
          case Lookaround(Ahead, cond, value) =>
            val rightSide: Node = cond match {
              case Positive => Intersection(Juxt(suffix), Juxt(Seq(value, wildcard)))
              case Negative => Difference(Juxt(suffix), Juxt(Seq(value, wildcard)))
            }
            if (prefix.isEmpty)
              fromTreeImpl(rightSide, from, to)
            else
              fromTreeImpl(Juxt(prefix :+ rightSide), from, to)
          case Lookaround(Behind, cond, value) =>
            val leftSide: Node = cond match {
              case Positive => Intersection(Juxt(prefix), Juxt(Seq(value, wildcard)))
              case Negative => Difference(Juxt(prefix), Juxt(Seq(value, wildcard)))
            }
            if (suffix.isEmpty)
              fromTreeImpl(leftSide, from, to)
            else
              fromTreeImpl(Juxt(leftSide +: suffix), from, to)
        }
      case None =>
        processJuxtNoLookaround(juxt, from, to)
    }
  }

  def findLookaround(args: Seq[Node]): Option[Int] = {
    val found = args.zipWithIndex.find { case (x, i) => x.isInstanceOf[Lookaround] }
    found.map { case (_, idx) => idx }
  }

  /**
    * Optimization: combination of consecutive negative lookahead constructions
    * (?!a)(?!b)(?!c) gets combined to (?!a|b|c), which is faster to process.
    * This optimization should be applied before the look-around's are expanded to intersections and differences.
    */
  def combineNegLookaheads(juxt: Juxt): Juxt = {
    import Direction._
    import Condition._
    val newValues = juxt.values.foldLeft(Seq[Node]()) { (acc, x) =>
      (acc, x) match {
        case (init :+ Lookaround(Ahead, Negative, v1), Lookaround(Ahead, Negative, v2)) =>
          init :+ Lookaround(Ahead, Negative, Disj(Seq(v1, v2)))
        case _ =>
          acc :+ x
      }
    }
    Juxt(newValues)
  }

  private def processJuxtNoLookaround(juxt: Juxt, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    juxt match {
      case Juxt(Seq()) =>
        Seq(new Nfa.Transition(from, to, Epsilon.instance))

      case Juxt(Seq(head)) =>
        fromTreeImpl(head, from, to)

      case Juxt(init :+ last) =>
        // doing this iteratively prevents stack overflows in the case of long literal strings
        val transitions = Buffer[Nfa.Transition]()
        var prev = from
        for (part <- init) {
          val int = new SimpleState
          transitions ++= fromTreeImpl(part, prev, int)
          prev = int
        }
        transitions ++= fromTreeImpl(last, prev, to)
        transitions.to(Seq)
    }
  }

  private def processDisj(disj: Disj, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    disj match {
      case Disj(Seq()) =>
        Seq()
      case Disj(parts) =>
        parts.map(part => fromTreeImpl(part, from, to)).flatten
    }
  }

  private def processRep(rep: Rep, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    rep match {

      // trivial cases

      case Rep(1, Some(1), value) =>
        fromTreeImpl(value, from, to)

      case Rep(0, Some(0), value) =>
        Seq(new Nfa.Transition(from, to, Epsilon.instance))

      // infinite repetitions

      case Rep(n, None, value) if n > 1 =>
        val juxt = Juxt(Seq.fill(n)(value) :+ Rep(0, None, value))
        fromTreeImpl(juxt, from, to)

      case Rep(1, None, value) =>
        val int1 = new SimpleState
        val int2 = new SimpleState
        fromTreeImpl(value, int1, int2) :+
          new Nfa.Transition(from, int1, Epsilon.instance) :+
          new Nfa.Transition(int2, to, Epsilon.instance) :+
          new Nfa.Transition(int2, int1, Epsilon.instance)

      case Rep(0, None, value) =>
        val int1 = new SimpleState
        val int2 = new SimpleState
        fromTreeImpl(value, int1, int2) :+
          new Nfa.Transition(from, int1, Epsilon.instance) :+
          new Nfa.Transition(int2, to, Epsilon.instance) :+
          new Nfa.Transition(from, to, Epsilon.instance) :+
          new Nfa.Transition(int2, int1, Epsilon.instance)

      // finite repetitions

      case Rep(n, Some(m), value) if n > 1 =>
        val x = n - 1
        val juxt = Juxt(Seq.fill(x)(value) :+ Rep(1, Some(m - x), value))
        fromTreeImpl(juxt, from, to)

      case Rep(1, Some(m), value) if m > 0 =>
        // doing this iteratively prevents stack overflows in the case of long repetitions
        val int1 = new SimpleState
        val transitions = Buffer[Nfa.Transition]()
        transitions ++= fromTreeImpl(value, from, int1)
        var prev = int1
        for (i <- 1 until m - 1) {
          val int = new SimpleState
          transitions ++= fromTreeImpl(value, prev, int)
          transitions += new Nfa.Transition(prev, to, Epsilon.instance)
          prev = int
        }
        transitions ++= fromTreeImpl(value, prev, to)
        transitions += new Nfa.Transition(prev, to, Epsilon.instance)
        transitions.to(Seq)

      case Rep(0, Some(m), value) if m > 0 =>
        // doing this iteratively prevents stack overflows in the case of long repetitions
        val transitions = Buffer[Nfa.Transition]()
        var prev = from
        for (i <- 0 until m - 1) {
          val int = new SimpleState
          transitions ++= fromTreeImpl(value, prev, int)
          transitions += new Nfa.Transition(prev, to, Epsilon.instance)
          prev = int
        }
        transitions ++= fromTreeImpl(value, prev, to)
        transitions += new Nfa.Transition(prev, to, Epsilon.instance)
        transitions.to(Seq)

    }
  }

  private def processOp(
      operation: DfaAlgorithms.BinaryOp[SimpleState],
      left: Node,
      right: Node,
      from: SimpleState,
      to: SimpleState): Seq[Nfa.Transition] = {
    val leftDfa = fromTree(left)
    val rightDfa = fromTree(right)
    val result = DfaAlgorithms.toNfa(operation(leftDfa, rightDfa))
    result.transitions.asScala ++
      result.accepting.asScala.to(Seq).map(acc => new Nfa.Transition(acc, to, Epsilon.instance)) ++
      Seq(new Nfa.Transition(from, result.initial, Epsilon.instance))
  }.toSeq

  def processCaptureGroup(value: Node, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    val int1 = new SimpleState
    val int2 = new SimpleState
    fromTreeImpl(value, int1, int2) :+
      new Nfa.Transition(from, int1, Epsilon.instance) :+
      new Nfa.Transition(int2, to, Epsilon.instance)
  }

}
