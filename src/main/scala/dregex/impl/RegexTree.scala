package dregex.impl

import scala.runtime.ScalaRunTime

sealed trait Direction
object Direction {
  case object Behind extends Direction
  case object Ahead extends Direction
}

sealed trait Condition
object Condition {
  case object Positive extends Condition
  case object Negative extends Condition
}

object RegexTree {

  sealed trait Node {
    def toRegex: String
    def canonical: Node
    def precedence: Int
  }

  sealed trait ComplexPart extends Node {
    def values: Seq[Node]
  }

  /**
    * A single char, non empty, i.e, excluding epsilon values
    */
  sealed trait AbstractRange extends Node with Product2[UnicodeChar, UnicodeChar] {

    def from: UnicodeChar
    def to: UnicodeChar

    def _1 = from
    def _2 = to

    override def equals(anyThat: Any) = anyThat match {
      case that: AbstractRange => this.from == that.from && this.to == that.to
      case _                   => false
    }

    override def hashCode() = ScalaRunTime._hashCode(this)

    def canonical = {
      (from, to) match {
        case (a, b) if a == b                   => Lit(a)
        case (UnicodeChar.min, UnicodeChar.max) => Wildcard
        case (a, b)                             => CharRange(a, b)
      }
    }

    def toCharClassLit: String

    def size = to.codePoint - from.codePoint + 1

  }

  case class CharRange(from: UnicodeChar, to: UnicodeChar) extends AbstractRange {

    if (from > to)
      throw new IllegalArgumentException("from value cannot be larger than to")

    def toRegex = throw new UnsupportedOperationException("Cannot express a range outside a char class")
    def toCharClassLit = s"${from.toRegex}-${to.toRegex}"
    def precedence = throw new UnsupportedOperationException("Cannot express a range outside a char class")
    override def toString = s"$from–$to"

  }

  case class Lit(char: UnicodeChar) extends AbstractRange with Product1[UnicodeChar] {
    // Product1 extended to override toString
    def from = char
    def to = char
    def toRegex = char.toRegex
    def toCharClassLit = char.toRegex
    def precedence = 1
    override def toString = char.toString
  }

  case object Wildcard extends AbstractRange {
    def from = UnicodeChar.min
    def to = UnicodeChar.max
    def toRegex = "."
    def toCharClassLit = throw new UnsupportedOperationException("Cannot express a wildcard inside a char class")
    def precedence = 1
    override def toString = "✶"
  }

  case class CharSet(ranges: Seq[AbstractRange]) extends Node {
    lazy val complement = CharSet(RangeOps.diff(Wildcard, ranges))
    def toRegex = s"[${ranges.map(_.toCharClassLit).mkString}]"
    def canonical = this
    def precedence = 1
    override def toString = s"${getClass.getSimpleName}(${ranges.mkString(",")})"
  }

  object CharSet {
    def fromCharSets(charSets: CharSet*): CharSet = CharSet(charSets.to(Seq).flatMap(_.ranges))
    def fromRange(interval: AbstractRange) = CharSet(Seq(interval))
  }

  case class Disj(values: Seq[Node]) extends ComplexPart {

    override def toString = s"${getClass.getSimpleName}(${values.mkString(",")})"

    def toRegex = values.map(_.toRegex).mkString("|")

    override def canonical = {
      def flattenValues(values: Seq[Node]): Seq[Node] = {
        values.flatMap { value =>
          value match {
            case Disj(innerValues) => flattenValues(innerValues)
            case other             => Seq(other)
          }
        }
      }
      Disj(flattenValues(values).map(_.canonical))
    }

    def precedence = 4

  }

  case class Lookaround(dir: Direction, cond: Condition, value: Node) extends ComplexPart {

    val values = Seq(value)

    def toRegex = {
      val dirStr = dir match {
        case Direction.Ahead  => ""
        case Direction.Behind => "<"
      }
      val condStr = cond match {
        case Condition.Negative => "!"
        case Condition.Positive => "="
      }
      s"(?$dirStr$condStr${value.toRegex})"
    }

    def canonical = Lookaround(dir, cond, value.canonical)
    def precedence = 1

  }

  case class Rep(min: Int, max: Option[Int], value: Node) extends ComplexPart {

    if (min < 0)
      throw new IllegalArgumentException

    max match {
      case Some(n) =>
        if (min > n)
          throw new IllegalArgumentException
      case None => // ok
    }

    val values = Seq(value)

    def toRegex = {
      val suffix = (min, max) match {
        case (0, None)              => "*"
        case (1, None)              => "+"
        case (n, None)              => s"{$n,}"
        case (0, Some(1))           => "?"
        case (1, Some(1))           => ""
        case (n, Some(m)) if n == m => s"{$n}"
        case (n, Some(m)) if n != m => s"{$n,$m}"
      }
      /*
       * On top of precedence, check special case of nested repetitions,
       * that are actually a grammar singularity. E.g., "a++" (invalid)
       * vs. "(a+)+" (valid)
       */
      val effValue =
        if (value.precedence > this.precedence || value.isInstanceOf[Rep])
          s"(?:${value.toRegex})"
        else
          value.toRegex
      s"$effValue$suffix"
    }

    override def canonical = {
      (min, max) match {
        case (1, Some(1)) => value.canonical
        case other        => Rep(min, max, value.canonical)
      }
    }

    def precedence = 2

    override def toString = {
      val range = (min, max) match {
        case (mn, None)                 => s"$mn–∞"
        case (mn, Some(mx)) if mn == mx => mn
        case (mn, Some(mx))             => s"$mn–$mx"
      }
      s"${getClass.getSimpleName}($range,$value)"
    }
  }

  case class Juxt(values: Seq[Node]) extends ComplexPart {

    override def toString = s"Juxt(${values.mkString(",")})"

    override def canonical = {
      def flattenValues(values: Seq[Node]): Seq[Node] = {
        values.flatMap { value =>
          value match {
            case Juxt(innerValues) => flattenValues(innerValues)
            case other             => Seq(other)
          }
        }
      }
      Juxt(flattenValues(values).map(_.canonical))
    }

    def toRegex = {
      val effValues = for (value <- values) yield {
        if (value.precedence > this.precedence)
          s"(?:${value.toRegex})"
        else
          value.toRegex
      }
      effValues.mkString
    }

    def precedence = 3

  }

  sealed trait Operation extends ComplexPart {
    def left: Node
    def right: Node
    def values = Seq(left, right)
    def toRegex = throw new UnsupportedOperationException("no regex expression for an operation")
    def precedence = throw new UnsupportedOperationException("no regex precedence for an operation")
  }

  case class Union(left: Node, right: Node) extends Operation {
    def canonical = Union(left.canonical, right.canonical)
  }

  case class Intersection(left: Node, right: Node) extends Operation {
    def canonical = Intersection(left.canonical, right.canonical)
  }

  case class Difference(left: Node, right: Node) extends Operation {
    def canonical = Difference(left.canonical, right.canonical)
  }

  sealed trait CaptureGroup extends ComplexPart {
    def value: Node
    override def values = Seq(value)
    override def canonical = this
    override def precedence = 1
  }

  case class PositionalCaptureGroup(value: Node) extends CaptureGroup {
    override def toRegex = s"(${value.toRegex})"
  }

  case class NamedCaptureGroup(name: String, value: Node) extends CaptureGroup {
    override def toRegex = s"(?<$name>${value.toRegex})"
  }

}
