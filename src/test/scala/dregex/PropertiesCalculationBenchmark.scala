package dregex

import dregex.impl.PredefinedCharSets
import org.scalatest.funsuite.AnyFunSuite

class PropertiesCalculationBenchmark extends AnyFunSuite {

  test("unicode literals") {
    // force "lazy val" evaluation
    PredefinedCharSets.allUnicodeLit
  }

  test("java classes ") {
    // force "lazy val" evaluation
    PredefinedCharSets.javaClasses
  }

  test("unicode general categories") {
    // force "lazy val" evaluation
    PredefinedCharSets.unicodeGeneralCategories
  }

  test("unicode properties") {
    // force "lazy val" evaluation
    PredefinedCharSets.unicodeBinaryProperties
  }

}
