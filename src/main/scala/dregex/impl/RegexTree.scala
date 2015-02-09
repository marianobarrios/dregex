package dregex.impl

object Direction extends Enumeration {
  val Behind, Ahead = Value
}

object Condition extends Enumeration {
  val Positive, Negative = Value
}

object RegexTree {

  trait Node {
    def hasLookarounds(): Boolean
    def length(): Option[Int]
  }

  trait ComplexPart extends Node {
    def values: Seq[Node]
    def hasLookarounds = !values.forall(!_.hasLookarounds)
    def map(fn: Node => Node): ComplexPart 
  }

  trait SingleComplexPart extends ComplexPart {
    def value: Node
    def values = Seq(value)
  }

  trait AtomPart extends Node {
    def atoms: Seq[Char]
    def hasLookarounds = false
  }

  case class Lit(char: Char) extends AtomPart {
    override def toString = char.toString
    def atoms = Seq(char)
    val length = Some(1)
  }

  case object Epsilon extends AtomPart {
    override def toString = "ε"
    val atoms = Seq()
    val length = Some(0)
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
    val length = Some(1)
  }

  case class CharClass(sets: CharSet*) extends AtomPart {
    def atoms = sets.map(_.chars).flatten
    val length = Some(1)
  }

  case class NegatedCharClass(sets: CharSet*) extends AtomPart {
    def atoms = sets.map(_.chars).flatten
    val length = Some(1)
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
    
    def length = {
      val lengths = values.map(_.length).collect { case Some(i) => i }
      if (lengths.size == values.size && lengths.toSet.size == 1)
        Some(lengths.head)
      else
        None
    }
    
    def map(fn: Node => Node) = Disj(values.map(fn))
    
  }

  case class Lookaround(dir: Direction.Value, cond: Condition.Value, value: Node) extends SingleComplexPart {
    override def hasLookarounds = true
    def length = throw new AssertionError
    def map(fn: Node => Node) = copy(value = fn(value))
    
  }

  case class Rep(min: Int, max: Int, value: Node) extends SingleComplexPart {
    
    def length = (min, max) match {
      case (_, -1) => None
      case (n, m) if n == m => Some(n)
      case (_, _) => None
    }
    
    def map(fn: Node => Node) = copy(value = fn(value))
    
  }

  case class Juxt(values: Seq[Node]) extends ComplexPart {
    
    override def toString = s"Juxt(${values.mkString(", ")})"
    
    def length = {
      val lengths = values.map(_.length).collect { case Some(i) => i }
      if (lengths.size == values.size)
        Some(lengths.sum)
      else
        None
    }
    
    def map(fn: Node => Node) = Juxt(values.map(fn))
    
  }

}
