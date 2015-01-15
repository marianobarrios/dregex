package dregex

trait RegexPart

case class Lit(char: Char) extends RegexPart {
  override def toString = char.toString
}

object Lit {
  def apply(str: String) = {
    if (str.length != 1)
      throw new Exception("String is no char: " + str)
    new Lit(str.head)
  }
}

case class Wildcard() extends RegexPart

case class CharClass(chars: Seq[Lit]) extends RegexPart
case class NegatedCharClass(chars: Seq[Lit]) extends RegexPart

object Direction extends Enumeration { 
  val Behind, Ahead = Value
}

object Condition extends Enumeration { 
  val Positive, Negative = Value
}

object Cardinality extends Enumeration {
  val ZeroToOne, ZeroToInf, OneToInf = Value
}

case class Disjunction(parts: Seq[RegexPart]) extends RegexPart {
  override def toString = s"Disjunction(${parts.mkString(", ")})"
}

case class Lookaround(dir: Direction.Value, cond: Condition.Value, value: RegexPart) extends RegexPart
case class Quantified(card: Cardinality.Value, value: RegexPart) extends RegexPart
case class Repetition(min: Int, max: Int, value: RegexPart) extends RegexPart

case class Juxtaposition(values: Seq[RegexPart]) extends RegexPart {
  override def toString = s"Juxtaposition(${values.mkString(", ")})"
}
