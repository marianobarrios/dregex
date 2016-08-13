package dregex

import dregex.impl.RegexTree
import scala.collection.mutable.ArrayBuffer
import dregex.impl.UnicodeChar

/**
 * Generates, given a regex tree, sample strings that match the regex.
 */
object StringGenerator {

  import RegexTree._

  def generate(regex: Node, maxAlternatives: Int, maxRepeat: Int): Seq[String] = {
    regex match {

      case CharSet(ranges) =>
        val gen = for {
          range <- ranges
        } yield {
          generate(range, maxAlternatives, maxRepeat)
        }
        gen.flatten

      case range: AbstractRange =>
        val length = math.min(maxAlternatives, range.size)
        for {
          i <- 0 until length
        } yield {
          UnicodeChar(range.from.codePoint + i).toJavaString
        }
        
      case Disj(values) =>
        values.flatMap(v => generate(v, maxAlternatives, maxRepeat))

      case Rep(min, maxOpt, value) =>
        import scala.util.control.Breaks._
        val max = maxOpt.getOrElse(Int.MaxValue - 1)
        var count = 0
        val res = ArrayBuffer[String]()
        breakable {
          for (i <- min to max) {
            res ++= fixedRepeat(value, maxAlternatives, maxRepeat, i)
            count += 1
            if (count >= maxRepeat)
              break()
          }
        }
        res

      case Juxt(Seq()) =>
        Seq()

      case Juxt(Seq(value)) =>
        generate(value, maxAlternatives, maxRepeat)
        
      case Juxt(first +: rest) =>
        for {
          left <- generate(first, maxAlternatives, maxRepeat)
          right <- generate(Juxt(rest), maxAlternatives, maxRepeat)
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