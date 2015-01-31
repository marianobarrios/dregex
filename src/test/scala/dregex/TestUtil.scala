package dregex

object TestUtil {
  
  def using[A, B](a: A)(fn: A => B) = fn(a)
  
}