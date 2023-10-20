package dregex;

import dregex.impl.CharInterval;
import dregex.impl.tree.CharRange;
import dregex.impl.tree.AbstractRange;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CharIntervalTest {

  @Test
  void testNonOverlapping() {
    List<AbstractRange> ranges = List.of(
            new CharRange(10, 20),
            new CharRange(21, 30),
            new CharRange(0, 100),
            new CharRange(9, 9),
            new CharRange(10, 11),
            new CharRange(9, 10),
            new CharRange(10, 12),
            new CharRange(17, 25));
    var nonOverlapping = CharInterval.calculateNonOverlapping(ranges);
    var expected = Map.of(
      new CharRange(10, 20), List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11),
        new CharInterval(12, 12),
        new CharInterval(13, 16),
        new CharInterval(17, 20)),
      new CharRange(21, 30), List.of(
        new CharInterval(21, 25),
        new CharInterval(26, 30)),
      new CharRange(0, 100), List.of(
        new CharInterval(0, 8),
        new CharInterval(9, 9),
        new CharInterval(10, 10),
        new CharInterval(11, 11),
        new CharInterval(12, 12),
        new CharInterval(13, 16),
        new CharInterval(17, 20),
        new CharInterval(21, 25),
        new CharInterval(26, 30),
        new CharInterval(31, 100)),
      new CharRange(9, 9), List.of(
        new CharInterval(9, 9)),
      new CharRange(10, 11), List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11)),
      new CharRange(9, 10), List.of(
        new CharInterval(9, 9),
        new CharInterval(10, 10)),
      new CharRange(10, 12), List.of(
        new CharInterval(10, 10),
        new CharInterval(11, 11),
        new CharInterval(12, 12)),
      new CharRange(17, 25), List.of(
        new CharInterval(17, 20),
        new CharInterval(21, 25))
    );
    assertEquals(expected, nonOverlapping);
  }

}
