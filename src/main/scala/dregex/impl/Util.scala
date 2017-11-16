package dregex.impl

import java.time.Duration

import scala.collection.immutable.SortedMap
import scala.collection.JavaConverters._

object Util {

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
  
  def time(thunk: => Unit): Duration = {
    val start = System.nanoTime()
    thunk
    Duration.ofNanos(System.nanoTime() - start)
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
  
  def getPrivateStaticField[A](clazz: Class[_], name: String): A = {
    val field = clazz.getDeclaredField(name)
    field.setAccessible(true)
    field.get(null).asInstanceOf[A]
  }

  def toSubscriptString(number: Int): String = {
    val string = number.toString
    val subScriptString = string.codePoints.iterator.asScala.flatMap { codePoint =>
      Character.toChars(codePoint + 8272).toSeq
    }
    new String(subScriptString.toArray)
  }
  
}