package dregex.impl

import java.util.concurrent.atomic.AtomicInteger

class State() {
  val id = State.counter.getAndIncrement()
  override def toString() = s"s$id"
}

object State {
  private val counter = new AtomicInteger
  val NullState = new State
}