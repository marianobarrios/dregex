package dregex

import org.scalatest.FunSuite
import dregex.impl.Util
import java.util.regex.Pattern
import scala.collection.immutable.Seq

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
        ))
    }
    info(s"compilation time: $elapsed1")
    val elapsed2 = Util.time {
      regexes.tail.foreach(_ doIntersect regexes.head)
    }
    info(s"intersection time: $elapsed2")
  }
  
  private def compare(regex: String, text: String, shouldMatch: Boolean): Unit = {
    info(s"matching '$text' with /$regex/")
    val nfa = Pattern.compile(regex)
    val dfa = Regex.compile(regex)
    val nfaElapsed = Util.time {
      assertResult(shouldMatch)(nfa.matcher(text).matches())
    }
    info(s"NFA (java.util.regex) time: $nfaElapsed")
    val dfaElapsed = Util.time {
      assertResult(shouldMatch)(dfa.matches(text))
    }
    info(s"DFA (dregex) time: $dfaElapsed")
  }
  
  test("NFA comparison") {
    compare(regex = "(x+x+)+y", text = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx", shouldMatch = false)
    compare(regex = "(.*?,){27}P", text = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27", shouldMatch = false)
  }
  
  test("large character classes") {
    val (regex, elapsed) = Util.time {
      Regex.compile("""[\x{0}-\x{10FFFF}]""")
    }
    info(s"compilation time: $elapsed")
  }

}