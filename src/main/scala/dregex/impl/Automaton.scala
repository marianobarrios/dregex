package dregex.impl

trait Transition[A, B] {
  def from: A
  def to: A
  def char: B
}

trait Automaton[A, B] {

  def allStates: Set[A]

  def transitions: Seq[Transition[A, B]]

  def initial: A

  def accepting: Set[A]

}