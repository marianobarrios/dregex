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

    //    def length = {
    //      val lengths = values.map(_.length).collect { case Some(i) => i }
    //      if (lengths.size == values.size && values.toSet.size == 1)
    //        Some(lengths.head)
    //      else
    //        None
    //    }

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

  case class CharClass(chars: Seq[Lit]) extends AtomPart {
    def atoms = chars.map(_.char)
    val length = Some(1)
  }

  case class NegatedCharClass(chars: Seq[Lit]) extends AtomPart {
    def atoms = chars.map(_.char)
    val length = Some(1)
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
  }

  case class Lookaround(dir: Direction.Value, cond: Condition.Value, value: Node) extends SingleComplexPart {
    override def hasLookarounds = true
    def length = throw new AssertionError
  }

  case class Rep(min: Int, max: Int, value: Node) extends SingleComplexPart {
    def length = (min, max) match {
      case (_, -1) => None
      case (n, m) if n == m => Some(n)
      case (_, _) => None
    }
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
  }

}
