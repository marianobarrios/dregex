package dregex

import scala.collection.mutable.ArrayBuffer
import dregex.impl.tree.Node
import dregex.impl.tree.AbstractRange
import dregex.impl.tree.CharSet
import dregex.impl.tree.Disj
import dregex.impl.tree.Rep
import dregex.impl.tree.Juxt
import scala.jdk.CollectionConverters._

/**
  * Generates, given a regex tree, sample strings that match the regex.
  */
object StringGenerator {

  def generate(regex: Node, maxAlternatives: Int, maxRepeat: Int): Seq[String] = {
    regex match {

      case set: CharSet =>
        val gen = for {
          range <- set.ranges.asScala.toSeq
        } yield {
          generate(range, maxAlternatives, maxRepeat)
        }
        gen.flatten

      case range: AbstractRange =>
        val length = math.min(maxAlternatives, range.to - range.from + 1)
        for {
          i <- 0 until length
        } yield {
          new String(Character.toChars(range.from + i))
        }

      case disj: Disj =>
        disj.values.asScala.toSeq.flatMap(v => generate(v, maxAlternatives, maxRepeat))

      case rep: Rep =>
        import scala.util.control.Breaks._
        val max = rep.max.orElseGet(() => Int.MaxValue - 1)
        var count = 0
        val res = ArrayBuffer[String]()
        breakable {
          for (i <- rep.min to max) {
            res ++= fixedRepeat(rep.value, maxAlternatives, maxRepeat, i)
            count += 1
            if (count >= maxRepeat)
              break()
          }
        }
        res.toSeq

      case juxt: Juxt if juxt.values.isEmpty() =>
        Seq()

      case juxt: Juxt if juxt.values.size() == 1 =>
        generate(juxt.values.get(0), maxAlternatives, maxRepeat)

      case juxt: Juxt if juxt.values.size() > 0 =>
        for {
          left <- generate(juxt.values.get(0), maxAlternatives, maxRepeat)
          right <- generate(new Juxt(juxt.values.subList(1, juxt.values.size() - 1)), maxAlternatives, maxRepeat)
        } yield {
          left + right
        }

      case other =>
        throw new RuntimeException("Unsupported node type: " + other.getClass)
    }
  }

  def fixedRepeat(value: Node, maxAlternatives: Int, maxRepeat: Int, qtty: Int): Seq[String] = {
    /*
     * To avoid a too fast explosion of combinations, we limit the number of
     * alternatives and repetitions to 1 inside repetitions to all but one
     * instance.
     */
    qtty match {
      case 0 =>
        Seq()
      case 1 =>
        generate(value, maxAlternatives, maxRepeat)
      case n =>
        for {
          left <- generate(value, maxAlternatives = 1, maxRepeat = 1)
          right <- fixedRepeat(value, maxAlternatives, maxRepeat, qtty - 1)
        } yield {
          left + right
        }
    }
  }

}
