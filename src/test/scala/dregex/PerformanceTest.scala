package dregex

import org.scalatest.FunSuite
import dregex.impl.Util

class PerformanceTest extends FunSuite {
  
    test("slow regexs") {
      val (regexes, elapsed1) = Util.time {
        Regex.compile(Seq(
            "qwertyuiopasd",
            "/aaaaaa/(?!xxc)(?!xxd)(?!xxe)(?!xxf)(?!xxg)(?!xxh).*",
            "/aaaaaa/(?!x+c)(?!x+d)(?!x+e)(?!x+f)(?!x+g)(?!x+h).*",
            "/aaaaaa/(?!x+c|x+d|x+e|x+f|x+g|x+h).*",
            "/aaaaaa/(?!xxc)a(?!xxd)b(?!xxe)c(?!xxf).*", // disables lookahead combinations
            "/aaaaaa/(?!xxc|xxd|xxe|xxf|xxg|xxh).*"
          )).unzip._2
      }
      info(s"compilation time: ${elapsed1 / 1000} ms")
      val (_, elapsed2) = Util.time {
        regexes.tail.foreach(_ doIntersect regexes.head)
      }
      info(s"intersection time: ${elapsed2 / 1000} ms")
    }

}