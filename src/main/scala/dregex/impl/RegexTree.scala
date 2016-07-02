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

  /**
   * A single char or null char, includes epsilon values and character classes
   */
  sealed trait SimplePart extends Node
  
  /**
   * A single or null char, i.e., including epsilon values
   */
  sealed trait AtomPart extends SimplePart 
  
  /**
   * A single char, non empty, i.e, excluding epsilon values
   */
  sealed trait NonEmptyChar extends AtomPart

  case object Other extends NonEmptyChar {
    override def toString = "other"
  }

  case class Lit(char: UnicodeChar) extends NonEmptyChar {
    override def toString = char.toString
  }

  object Lit {
    
    def fromChar(char: Char) = {
      Lit(UnicodeChar(char))
    }
    
    def fromSingletonString(str: String) = {
      if (Character.codePointCount(str, 0, str.size) > 1)
        throw new IllegalAccessException("String is no char: " + str)
      Lit(UnicodeChar(Character.codePointAt(str, 0)))
    }
    
  }

  case object Epsilon extends AtomPart {
    override def toString = "ε"
  }

  sealed trait ExpandiblePart extends SimplePart 
  
  case object Wildcard extends ExpandiblePart
  
  case class CharClass(sets: CharSet*) extends ExpandiblePart
  case class NegatedCharClass(sets: CharSet*) extends ExpandiblePart

  trait CharSet {
    def chars: Seq[UnicodeChar]
    def resolve(alphabet: Set[NonEmptyChar]): Set[NonEmptyChar]
  }

  case class CompCharSet(charSet: CharSet) extends CharSet {
    def chars = charSet.chars
    def resolve(alphabet: Set[NonEmptyChar]) = alphabet diff charSet.resolve(alphabet)
  }

  case class ExtensionCharSet(chars: UnicodeChar*) extends CharSet {
    def resolve(alphabet: Set[NonEmptyChar]) = chars.map(Lit(_)).toSet
  }
  
  object ExtensionCharSet {
    def fromCharLiterals(chars: Char*) = {
      ExtensionCharSet(chars.map(UnicodeChar(_)): _*)
    }
  }

  case class RangeCharSet(from: UnicodeChar, to: UnicodeChar) extends CharSet {
    val chars = (from.codePoint to to.codePoint).map(UnicodeChar(_)).toSeq
    def resolve(alphabet: Set[NonEmptyChar]) = (from.codePoint to to.codePoint).map(cp => Lit(UnicodeChar(cp))).toSet
  }
  
  object RangeCharSet {
    def fromCharLiterals(from: Char, to: Char) = {
      RangeCharSet(UnicodeChar(from), UnicodeChar(to))
    }
  }

  case class MultiRangeCharSet(ranges: CharSet*) extends CharSet {
    val chars = ranges.map(r => r.chars).flatten
    def resolve(alphabet: Set[NonEmptyChar]) = ranges.map(_.chars).flatten.map(Lit(_)).toSet
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
