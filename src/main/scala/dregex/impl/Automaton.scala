package dregex.impl

trait Automaton[A, B] {
  
  def allStates: Set[A]
  
  def transitions: Map[A, Map[B, Set[A]]]
  
  def initial: A
  
  def accepting: Set[A]
  
}