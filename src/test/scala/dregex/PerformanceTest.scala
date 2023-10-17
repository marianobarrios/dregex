package dregex

import org.junit.jupiter.api.Test

import java.time.Duration
import scala.jdk.CollectionConverters._

class PerformanceTest {

  @Test
  def testSlowRegexs() = {
    val start1 = System.nanoTime()
    val regexes = Regex.compile(
      java.util.List.of(
        "qwertyuiopasd",
        "/aaaaaa/(?!xxc)(?!xxd)(?!xxe)(?!xxf)(?!xxg)(?!xxh)[a-zA-Z0-9]{7}.*",
        "/aaaaaa/(?!x+c)(?!x+d)(?!x+e)(?!x+f)(?!x+g)(?!x+h)[a-zA-Z0-9]{7}.*",
        "/aaaaaa/(?!x+c|x+d|x+e|x+f|x+g|x+h)[a-zA-Z0-9]{7}.*",
        "/aaaaaa/(?!xxc)a(?!xxd)b(?!xxx)c(?!xxf)[a-zA-Z0-9]{7}.*", // disables lookahead combinations
        "/aaaaaa/(?!xxc|xxd|xxe|xxf|xxg|xxh)[a-zA-Z0-9]{7}.*",
        "/aaaaaa/(?!xxc.*)(?!xxd.*)(?!xxe.*)(?!xxf.*)(?!xxg.*)[a-zA-Z0-9]{7}.*"
      ))
    val elapsed1 = Duration.ofNanos(System.nanoTime() - start1)
    println(s"compilation time: $elapsed1")
    val start2 = System.nanoTime()
    regexes.asScala.tail.foreach(_ doIntersect regexes.asScala.head)
    val elapsed2 = Duration.ofNanos(System.nanoTime() - start2)
    println(s"intersection time: $elapsed2")
  }

  @Test
  def testLargeSharacterClasses() = {
    val start = System.nanoTime()
    val regex = Regex.compile("""[\x{0}-\x{10FFFF}]""")
    val elapsed = Duration.ofNanos(System.nanoTime() - start)
    println(s"compilation time: $elapsed")
  }

}
