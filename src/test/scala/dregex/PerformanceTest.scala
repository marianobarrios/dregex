package dregex

import org.scalatest.FunSuite
import dregex.impl.Util

class PerformanceTest extends FunSuite {

  test("slow regexs") {
    val (regexes, elapsed1) = Util.time {
      Regex.compile(Seq(
        "qwertyuiopasd",
        "/aaaaaa/(?!xxc)(?!xxd)(?!xxe)(?!xxf)(?!xxg)(?!xxh)[a-zA-Z0-9]{7}.*",
        "/aaaaaa/(?!x+c)(?!x+d)(?!x+e)(?!x+f)(?!x+g)(?!x+h)[a-zA-Z0-9]{7}.*",
        "/aaaaaa/(?!x+c|x+d|x+e|x+f|x+g|x+h)[a-zA-Z0-9]{7}.*",
        "/aaaaaa/(?!xxc)a(?!xxd)b(?!xxx)c(?!xxf)[a-zA-Z0-9]{7}.*", // disables lookahead combinations
        "/aaaaaa/(?!xxc|xxd|xxe|xxf|xxg|xxh)[a-zA-Z0-9]{7}.*",
        "/aaaaaa/(?!xxc.*)(?!xxd.*)(?!xxe.*)(?!xxf.*)(?!xxg.*)[a-zA-Z0-9]{7}.*"
        )).unzip._2
    }
    info(s"compilation time: $elapsed1")
    val (_, elapsed2) = Util.time {
      regexes.tail.foreach(_ doIntersect regexes.head)
    }
    info(s"intersection time: $elapsed2")
  }

}