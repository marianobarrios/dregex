package dregex.impl

object Direction extends Enumeration {
  val Behind, Ahead = Value
}

object Condition extends Enumeration {
  val Positive, Negative = Value
}

object RegexTree {

  sealed trait Node

  sealed trait ComplexPart extends Node {
    def values: Seq[Node]
  }

  sealed trait OneChildComplexPart extends ComplexPart {
    def value: Node
    def values = Seq(value)
  }

  sealed trait AtomPart extends Node
  
  sealed trait SglChar extends AtomPart

  case object Other extends SglChar {
    override def toString = "other"
  }

  case class Lit(char: Char) extends SglChar {
    override def toString = char.toString
  }

  object Lit {
    def apply(str: String) = {
      if (str.length != 1)
        throw new IllegalAccessException("String is no char: " + str)
      new Lit(str.head)
    }
  }

  case object Epsilon extends AtomPart {
    override def toString = "ε"
  }

  case object Wildcard extends AtomPart
  
  case class CharClass(sets: CharSet*) extends AtomPart
  case class NegatedCharClass(sets: CharSet*) extends AtomPart

  trait CharSet {
    def chars: Seq[Char]
    def resolve(alphabet: Set[SglChar]): Set[SglChar]
  }

  case class CompCharSet(charSet: CharSet) extends CharSet {
    def chars = charSet.chars
    def resolve(alphabet: Set[SglChar]) = alphabet diff charSet.resolve(alphabet)
  }

  case class ExtensionCharSet(chars: Char*) extends CharSet {
    def resolve(alphabet: Set[SglChar]) = chars.map(Lit(_)).toSet
  }

  case class RangeCharSet(from: Char, to: Char) extends CharSet {
    val chars = (from to to).toSeq
    def resolve(alphabet: Set[SglChar]) = (from to to).map(Lit(_)).toSet
  }

  case class MultiRangeCharSet(ranges: CharSet*) extends CharSet {
    val chars = ranges.map(r => r.chars).flatten
    def resolve(alphabet: Set[SglChar]) = ranges.map(_.chars).flatten.map(Lit(_)).toSet
  }

  case class Disj(values: Seq[Node]) extends ComplexPart {
    override def toString = s"Disj(${values.mkString(", ")})"
  }

  case class Lookaround(dir: Direction.Value, cond: Condition.Value, value: Node) extends OneChildComplexPart

  case class Rep(min: Int, max: Option[Int], value: Node) extends OneChildComplexPart

  case class Juxt(values: Seq[Node]) extends ComplexPart {
    override def toString = s"Juxt(${values.mkString(", ")})"
  }

  sealed trait Operation extends ComplexPart {
    def left: Node
    def right: Node
    def values = Seq(left, right)
  }

  case class Union(left: Node, right: Node) extends Operation
  case class Intersection(left: Node, right: Node) extends Operation
  case class Difference(left: Node, right: Node) extends Operation
  
}
