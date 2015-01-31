package dregex.impl

import java.util.concurrent.atomic.AtomicInteger

class State() {

  val id = {
    val c = State.counter.getAndIncrement()
    val base = 26
    def encode(n: Int): Seq[Int] = {
      val digit = n % base
      n / base match {
        case 0 => Seq(digit)
        case rest => encode(rest) :+ digit
      }
    }
    new String(encode(c).map(d => ('A' + d).toChar).toArray)
  }

  override def toString() = id

}

object State {
  private val counter = new AtomicInteger
  val NullState = new State
}