package dregex

object Util {

  def mergeNestedWithUnion[A, B, C](left: Map[A, Map[B, Set[C]]], right: Map[A, Map[B, Set[C]]]) = {
    merge(left, right)((l, r) => merge(l, r)(_ union _))
  }

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

}