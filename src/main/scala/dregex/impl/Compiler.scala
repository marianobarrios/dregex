package dregex.impl

import dregex.InvalidRegexException

import java.util.stream.Collectors
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters._
import dregex.impl.tree.{AbstractRange, CharSet, Condition, Difference, Direction, Disj, Intersection, Lookaround, NamedCaptureGroup, Node, Operation, PositionalCaptureGroup, Rep, Union, Wildcard, Juxt}

import java.util.Optional

/**
  * Take a regex AST and produce a NFA.
  * Except when noted the Thompson-McNaughton-Yamada algorithm is used.
  * Reference: http://stackoverflow.com/questions/11819185/steps-to-creating-an-nfa-from-a-regular-expression
  */
class Compiler(intervalMapping: java.util.Map[AbstractRange, java.util.List[CharInterval]]) {

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

      case set: CharSet =>
        fromTreeImpl(new Disj(set.ranges), from, to)

      case juxt: Juxt =>
        processJuxt(combineNegLookaheads(juxt), from, to)

      case la: Lookaround =>
        fromTreeImpl(new Juxt(java.util.List.of(la)), from, to)

      case disj: Disj =>
        processDisj(disj, from, to)

      case rep: Rep =>
        processRep(rep, from, to)

      case intersection: Intersection =>
        processOp(DfaAlgorithms.doIntersect, intersection.left, intersection.right, from, to)

      case union: Union =>
        processOp(DfaAlgorithms.union, union.left, union.right, from, to)

      case difference: Difference =>
        processOp(DfaAlgorithms.diff, difference.left, difference.right, from, to)

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
    findLookaround(juxt.values.asScala.toSeq) match {
      case Some(i) =>
        val prefix = juxt.values.asScala.slice(0, i)
        val suffix = juxt.values.asScala.slice(i + 1, juxt.values.asScala.size)
        val wildcard = new Rep(0, Optional.empty(), Wildcard.instance)
        juxt.values.asScala(i).asInstanceOf[Lookaround] match {
          case lookaround if lookaround.dir == Ahead =>
            val rightSide: Node = lookaround.cond match {
              case Positive => new Intersection(new Juxt(suffix.asJava), new Juxt(java.util.List.of(lookaround.value, wildcard)))
              case Negative => new Difference(new Juxt(suffix.asJava), new Juxt(java.util.List.of(lookaround.value, wildcard)))
            }
            if (prefix.isEmpty)
              fromTreeImpl(rightSide, from, to)
            else
              fromTreeImpl(new Juxt((prefix :+ rightSide).asJava), from, to)
          case lookaround if lookaround.dir == Behind =>
            val leftSide: Node = lookaround.cond match {
              case Positive => new Intersection(new Juxt(prefix.asJava), new Juxt(java.util.List.of(lookaround.value, wildcard)))
              case Negative => new Difference(new Juxt(prefix.asJava), new Juxt(java.util.List.of(lookaround.value, wildcard)))
            }
            if (suffix.isEmpty)
              fromTreeImpl(leftSide, from, to)
            else
              fromTreeImpl(new Juxt((leftSide +: suffix).asJava), from, to)
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
    val newValues = juxt.values.asScala.foldLeft(Seq[Node]()) { (acc, x) =>
      if (!acc.isEmpty && acc.last.isInstanceOf[Lookaround] && x.isInstanceOf[Lookaround]) {
        val la1 = acc.last.asInstanceOf[Lookaround]
        val la2 = x.asInstanceOf[Lookaround]
        if (la1.dir == Ahead && la2.dir == Ahead) {
          if (la1.cond == Negative && la2.cond == Negative) {
            acc.init :+ new Lookaround(Ahead, Negative, new Disj(java.util.List.of(la1.value, la2.value)))
          } else {
            acc :+ x
          }
        } else {
          acc :+ x
        }
      } else {
        acc :+ x
      }
    }
    new Juxt(newValues.asJava)
  }

  private def processJuxtNoLookaround(juxt: Juxt, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    juxt match {
      case juxt: Juxt if juxt.values.isEmpty =>
        Seq(new Nfa.Transition(from, to, Epsilon.instance))

      case just: Juxt if just.values.size() == 1 =>
        fromTreeImpl(juxt.values.get(0), from, to)

      case just: Juxt =>
        // doing this iteratively prevents stack overflows in the case of long literal strings
        val transitions = Buffer[Nfa.Transition]()
        var prev = from
        for (part <- just.values.subList(0, just.values.size() - 1).asScala) {
          val int = new SimpleState
          transitions ++= fromTreeImpl(part, prev, int)
          prev = int
        }
        transitions ++= fromTreeImpl(just.values.get(juxt.values.size() - 1), prev, to)
        transitions.to(Seq)
    }
  }

  private def processDisj(disj: Disj, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    disj.values.asScala.toSeq.flatMap(part => fromTreeImpl(part, from, to))
  }

  private def processRep(rep: Rep, from: SimpleState, to: SimpleState): Seq[Nfa.Transition] = {
    rep match {

      // trivial cases

      case rep: Rep if rep.min == 1 && rep.max == Optional.of(1) =>
        fromTreeImpl(rep.value, from, to)

      case rep: Rep if rep.min == 0 && rep.max == Optional.of(0) =>
        Seq(new Nfa.Transition(from, to, Epsilon.instance))

      // infinite repetitions

      case rep: Rep if rep.min > 1 && rep.max.isEmpty =>
        val juxt = new Juxt((Seq.fill(rep.min)(rep.value) :+ new Rep(0, Optional.empty(), rep.value)).asJava)
        fromTreeImpl(juxt, from, to)

      case rep: Rep if rep.min == 1 && rep.max.isEmpty =>
        val int1 = new SimpleState
        val int2 = new SimpleState
        fromTreeImpl(rep.value, int1, int2) :+
          new Nfa.Transition(from, int1, Epsilon.instance) :+
          new Nfa.Transition(int2, to, Epsilon.instance) :+
          new Nfa.Transition(int2, int1, Epsilon.instance)

      case rep: Rep if rep.min == 0 && rep.max.isEmpty =>
        val int1 = new SimpleState
        val int2 = new SimpleState
        fromTreeImpl(rep.value, int1, int2) :+
          new Nfa.Transition(from, int1, Epsilon.instance) :+
          new Nfa.Transition(int2, to, Epsilon.instance) :+
          new Nfa.Transition(from, to, Epsilon.instance) :+
          new Nfa.Transition(int2, int1, Epsilon.instance)

      // finite repetitions

      case rep: Rep if rep.min > 1 && rep.max.isPresent =>
        val x = rep.min - 1
        val juxt = new Juxt((Seq.fill(x)(rep.value) :+ new Rep(1, Optional.of(rep.max.get() - x), rep.value)).asJava)
        fromTreeImpl(juxt, from, to)

      case rep: Rep if rep.min == 1 && rep.max.isPresent && rep.max.get() > 0 =>
        // doing this iteratively prevents stack overflows in the case of long repetitions
        val int1 = new SimpleState
        val transitions = Buffer[Nfa.Transition]()
        transitions ++= fromTreeImpl(rep.value, from, int1)
        var prev = int1
        for (i <- 1 until rep.max.get() - 1) {
          val int = new SimpleState
          transitions ++= fromTreeImpl(rep.value, prev, int)
          transitions += new Nfa.Transition(prev, to, Epsilon.instance)
          prev = int
        }
        transitions ++= fromTreeImpl(rep.value, prev, to)
        transitions += new Nfa.Transition(prev, to, Epsilon.instance)
        transitions.to(Seq)

      case rep: Rep if rep.min == 0 && rep.max.isPresent && rep.max.get() > 0 =>
        // doing this iteratively prevents stack overflows in the case of long repetitions
        val transitions = Buffer[Nfa.Transition]()
        var prev = from
        for (i <- 0 until rep.max.get() - 1) {
          val int = new SimpleState
          transitions ++= fromTreeImpl(rep.value, prev, int)
          transitions += new Nfa.Transition(prev, to, Epsilon.instance)
          prev = int
        }
        transitions ++= fromTreeImpl(rep.value, prev, to)
        transitions += new Nfa.Transition(prev, to, Epsilon.instance)
        transitions.to(Seq)

    }
  }

  private type BinaryOp[A <: State] = (Dfa[A], Dfa[A]) => Dfa[BiState[A]]

  private def processOp(
      operation: BinaryOp[SimpleState],
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
