package dregex

import scala.collection.immutable.Seq

object SetOpsExample {

    def main(args: Array[String]): Unit = {
        {
            val Seq((_, lower), (_, upper), (_, both)) = Regex.compile(Seq("[a-z]+", "[A-Z]+", "[a-z]+|[A-Z]+"))
            println(lower.doIntersect(upper)) // false
            println(both.equiv(lower.union(upper))) // true
        }
        {
            val Seq((_, all), (_, upper)) = Regex.compile(Seq("[a-z]+|[A-Z]+", "[A-Z]+"))
            val lower = all.diff(upper)
            println(lower.matches("aaa")) // true
            println(lower.matches("Aaa")) // false
        }
    }

}
