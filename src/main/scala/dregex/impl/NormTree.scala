package dregex.impl

object NormTree {

  sealed trait Node

  sealed trait Char extends Node
  sealed trait SglChar extends Char

  case object Other extends SglChar {
    override def toString = "other"
  }

  case class Lit(char: scala.Char) extends SglChar {
    override def toString = char.toString
  }

  case object Epsilon extends Char {
    override def toString = "ε"
  }

  case class Disj(values: Seq[Node]) extends Node {
    override def toString = s"Disj(${values.mkString(", ")})"
  }

  /**
   * A repetition of at least {@link #min} up to {@link #max}. 
   * If {@link #max} is {@link Option#None} it means infinite.
   */
  case class Rep(min: Int, max: Option[Int], value: Node) extends Node

  case class Juxt(values: Seq[Node]) extends Node {
    override def toString = s"Juxt(${values.mkString(", ")})"
  }
  
  case class Lookaround(dir: Direction.Value, cond: Condition.Value, value: Node) extends Node
  
  case class Union(left: Node, right: Node) extends Node
  case class Intersection(left: Node, right: Node) extends Node
  case class Difference(left: Node, right: Node) extends Node

}