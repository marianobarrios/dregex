package dregex.impl

import com.typesafe.scalalogging.slf4j.StrictLogging
import java.time.Duration
import scala.collection.immutable.SortedMap

object Util extends StrictLogging {

  def mergeWithUnion[B, C](left: Map[B, Set[C]], right: Map[B, Set[C]]) = merge(left, right)(_ union _)

  def merge[A, B](left: Map[A, B], right: Map[A, B])(fn: (B, B) => B): Map[A, B] = {
    val merged = for ((k, lv) <- left) yield {
      val mergedValue = right.get(k) match {
        case Some(rv) => fn(lv, rv)
        case None => lv
      }
      k -> mergedValue
    }
    merged ++ right.filterKeys(!left.contains(_))
  }

  def doIntersect[A](left: Set[A], right: Set[A]) = left exists right

  def time[A](thunk: => A): (A, Duration) = {
    val start = System.nanoTime()
    val res = thunk
    val time = Duration.ofNanos(System.nanoTime() - start)
    (res, time)
  }

  implicit class StrictSortedMap[K <: Ordered[K], A](map: SortedMap[K, A]) {
    def mapValuesNow[B](f: A => B): SortedMap[K, B] = {
      val a = map.toSeq.map { case (a, b) => (a, f(b)) }
      SortedMap(a: _*)
    }
  }
  
  implicit class StrictMap[K, A](map: Map[K, A]) {
    def mapValuesNow[B](f: A => B): Map[K, B] = map.map { case (a, b) => (a, f(b)) }
  }  
  
  def floorEntry[A, B](sortedMap: SortedMap[A, B], key: A): Option[(A, B)] = {
    sortedMap.to(key).lastOption
  }
  
}