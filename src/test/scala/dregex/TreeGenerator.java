package dregex;

import dregex.impl.database.PosixCharSets;
import dregex.impl.tree.Node;
import dregex.impl.tree.Lit;
import dregex.impl.tree.CharRange;
import dregex.impl.tree.Wildcard;
import dregex.impl.tree.CharSet;
import dregex.impl.tree.Disj;
import dregex.impl.tree.Rep;
import dregex.impl.tree.Juxt;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
  * Generate some sample regex trees, useful for testing.
  */
class TreeGenerator {

  private Stream<Node> generateFixedDepth(int levels) {
    if (levels == 1) {
      return Stream.of(
        new Lit('a'),
        Wildcard.instance,
        new CharSet(new CharRange('d', 'f')),
        new CharSet(new CharRange('d', 'f')).complement(),
        PosixCharSets.digit);
    } else {
      return generateFixedDepth(levels - 1).flatMap(  node -> {
        var simple = Stream.of(
          new Rep(0, Optional.empty(), node),
          new Rep(1, Optional.empty(), node),
          new Rep(2, Optional.empty(), node),
          new Rep(2, Optional.of(3), node),
          new Rep(0, Optional.of(1), node));
        var doubl = generateFixedDepth(levels - 1).flatMap(secondNode -> {
          return Stream.of(new Disj(node, secondNode), new Juxt(node, secondNode));
        });
        return Stream.concat(simple, doubl);
      });
    }
  }

  public Stream<Node> generate(int maxDepth) {
    return IntStream.range(1, maxDepth + 1).boxed().flatMap(i -> generateFixedDepth(i));
  }

}
