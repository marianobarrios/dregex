package dregex

import org.scalatest.FunSuite
import com.typesafe.scalalogging.slf4j.StrictLogging

class PerformanceTest extends FunSuite with StrictLogging {

  def time[A](thunk: => A): (A, Long) = {
    val start = System.nanoTime()
    val res = thunk
    val time = (System.nanoTime() - start) / 1000
    (res, time)
  }

  test("slow regexs") {
    val (_, elapsed) = time {
      Regex.compile("/aaaaaa/(?!xxc)(?!xxd)(?!xxe)(?!xxf)(?!xxg)(?!xxh).*")
      Regex.compile("/aaaaaa/(?!x+c)(?!x+d)(?!x+e)(?!x+f)(?!x+g)(?!x+h).*")
      Regex.compile("/aaaaaa/(?!x+c|x+d|x+e|x+f|x+g|x+h).*")
      Regex.compile("/aaaaaa.+/(?!xxc)(?!xxd)(?!xxe)(?!xxf)(?!xxg)(?!xxh).*")
      Regex.compile("/aaaaaa.+/(?!xxc)a(?!xxd)b(?!xxe)c(?!xxf).*")
      Regex.compile("/aaaaaa.+/(?!xxc|xxd|xxe|xxf|xxg|xxh).*")
    }
    logger.info(s"Performance test took ${elapsed / 1000} ms")
  }

}