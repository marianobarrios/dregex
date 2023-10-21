package dregex;

import dregex.impl.RangeOps;
import dregex.impl.tree.CharRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeOpsTest {

  private static class RangeComparator implements Comparator<CharRange> {

    @Override
    public int compare(CharRange a, CharRange b) {
      var from = Integer.compare(a.from, b.from);
      if (from != 0) {
        return from;
      } else {
        return Integer.compare(a.to, b.to);
      }
    }
  }

  @Test
  void testUnion()  {
    var ranges = new ArrayList<>(List.of(new CharRange(10, 20), new CharRange(9, 9), new CharRange(25, 28), new CharRange(3, 3),
            new CharRange(10, 11), new CharRange(9, 10), new CharRange(100, 100), new CharRange(101, 101)));
    ranges.sort(new RangeComparator());
    var union = RangeOps.union(ranges);
    var expected = List.of(new CharRange(3, 3), new CharRange(9, 20), new CharRange(25, 28), new CharRange(100, 101));
    assertEquals(expected, union);
  }

}
