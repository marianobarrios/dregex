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
  }

  trait ComplexPart extends Node {
    def values: Seq[Node]
    def hasLookarounds = !values.forall(!_.hasLookarounds)
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

  case class CharClass(chars: Seq[Lit]) extends AtomPart {
    def atoms = chars.map(_.char)
  }

  case class NegatedCharClass(chars: Seq[Lit]) extends AtomPart {
    def atoms = chars.map(_.char)
  }

  case class Disj(values: Seq[Node]) extends ComplexPart {
    override def toString = s"Disj(${values.mkString(", ")})"
  }

  case class Lookaround(dir: Direction.Value, cond: Condition.Value, value: Node) extends SingleComplexPart {
    override def hasLookarounds = true
  }

  case class Rep(min: Int, max: Int, value: Node) extends SingleComplexPart 

  case class Juxt(values: Seq[Node]) extends ComplexPart {
    override def toString = s"Juxt(${values.mkString(", ")})"
  }

}
