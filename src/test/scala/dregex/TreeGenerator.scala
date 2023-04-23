package dregex

import dregex.impl.database.PosixCharSets
import dregex.impl.tree.Node
import dregex.impl.tree.Lit
import dregex.impl.tree.CharRange
import dregex.impl.tree.Wildcard
import dregex.impl.tree.CharSet
import dregex.impl.tree.Disj
import dregex.impl.tree.Rep
import dregex.impl.tree.Juxt
import java.util.Optional
/**
  * Generate some sample regex trees, useful for testing.
  */
class TreeGenerator {

  private def generateFixedDepth(levels: Int): Iterator[Node] = {
    if (levels == 1) {
      Iterator(
        new Lit('a'),
        Wildcard.instance,
        new CharSet(new CharRange('d', 'f')),
        new CharSet(new CharRange('d', 'f')).complement,
        PosixCharSets.digit)
    } else {
      generateFixedDepth(levels - 1).flatMap { node =>
        val simple = Iterator(
          new Rep(0, Optional.empty(), node),
          new Rep(1, Optional.empty(), node),
          new Rep(2, Optional.empty(), node),
          new Rep(2, Optional.of(3), node),
          new Rep(0, Optional.of(1), node))
        val double = generateFixedDepth(levels - 1).flatMap { secondNode =>
          Iterator(new Disj(java.util.List.of(node, secondNode)), new Juxt(java.util.List.of(node, secondNode)))
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
