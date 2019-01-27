package dregex

import dregex.impl.RegexTree
import dregex.impl.UnicodeChar.FromCharConversion
import dregex.impl.PredefinedCharSets
import scala.collection.immutable.Seq

/**
  * Generate some sample regex trees, useful for testing.
  */
class TreeGenerator {

  import RegexTree._

  private def generateFixedDepth(levels: Int): Iterator[Node] = {
    if (levels == 1) {
      Iterator(
        Lit('a'.u),
        Wildcard,
        CharSet.fromRange(CharRange('d'.u, 'f'.u)),
        CharSet.fromRange(CharRange('d'.u, 'f'.u)).complement,
        PredefinedCharSets.digit)
    } else {
      generateFixedDepth(levels - 1).flatMap { node =>
        val simple = Iterator(
          Rep(0, None, node),
          Rep(1, None, node),
          Rep(2, None, node),
          Rep(2, Some(3), node),
          Rep(0, Some(1), node))
        val double = generateFixedDepth(levels - 1).flatMap { secondNode =>
          Iterator(Disj(Seq(node, secondNode)), Juxt(Seq(node, secondNode)))
        //Juxt(Seq(Lit('x'.u), Lookaround(Direction.Ahead, Condition.Negative, node), secondNode)),
        //Juxt(Seq(Lit('x'.u), Lookaround(Direction.Ahead, Condition.Positive, node), secondNode)))
        }
        simple ++ double
      }
    }
  }

  def generate(maxDepth: Int): Iterator[Node] = {
    Iterator.range(1, maxDepth + 1).flatMap { i =>
      generateFixedDepth(levels = i)
    }
  }

}
