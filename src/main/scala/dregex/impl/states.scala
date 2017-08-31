package dregex.impl

import java.util.concurrent.atomic.AtomicInteger

trait DfaState

class State() extends DfaState {
  val id = State.counter.getAndIncrement()
  override def toString() = s"s$id"
}

object State {
  private val counter = new AtomicInteger
}

case class BiState[A <: DfaState](first: A, second: A) extends DfaState  {
  override def toString() = {
    s"$first,$second"
  }
}

case class MultiState(states: Set[State]) extends DfaState  {
  override def toString() = {
    states.mkString(",")
  }
}