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

  sealed trait SingleComplexPart extends ComplexPart {
    def value: Node
    def values = Seq(value)
  }

  sealed trait AtomPart extends Node {
    def atoms: Seq[Char]
  }

  case class Lit(char: Char) extends AtomPart {
    override def toString = char.toString
    def atoms = Seq(char)
  }

  case object Epsilon extends AtomPart {
    override def toString = "ε"
    val atoms = Seq()
  }

  object Lit {
    def apply(str: String) = {
      if (str.length != 1)
        throw new IllegalAccessException("String is no char: " + str)
      new Lit(str.head)
    }
  }

  case object Wildcard extends AtomPart {
    val atoms = Seq()
  }

  case class CharClass(sets: CharSet*) extends AtomPart {
    def atoms = sets.map(_.chars).flatten
  }

  case class NegatedCharClass(sets: CharSet*) extends AtomPart {
    def atoms = sets.map(_.chars).flatten
  }

  trait CharSet {
    def chars: Seq[Char]
    def resolve(alphabet: Set[NormTree.SglChar]): Set[NormTree.SglChar]
  }

  case class CompCharSet(charSet: CharSet) extends CharSet {
    def chars = charSet.chars
    def resolve(alphabet: Set[NormTree.SglChar]) = alphabet diff charSet.resolve(alphabet)
  }

  case class ExtensionCharSet(chars: Char*) extends CharSet {
    def resolve(alphabet: Set[NormTree.SglChar]) = chars.map(NormTree.Lit(_)).toSet
  }

  case class RangeCharSet(from: Char, to: Char) extends CharSet {
    val chars = (from to to).toSeq
    def resolve(alphabet: Set[NormTree.SglChar]) = (from to to).map(NormTree.Lit(_)).toSet
  }

  case class MultiRangeCharSet(ranges: CharSet*) extends CharSet {
    val chars = ranges.map(r => r.chars).flatten
    def resolve(alphabet: Set[NormTree.SglChar]) = ranges.map(_.chars).flatten.map(NormTree.Lit(_)).toSet
  }

  case class Disj(values: Seq[Node]) extends ComplexPart {
    override def toString = s"Disj(${values.mkString(", ")})"
  }

  case class Lookaround(dir: Direction.Value, cond: Condition.Value, value: Node) extends SingleComplexPart

  case class Rep(min: Int, max: Option[Int], value: Node) extends SingleComplexPart

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
