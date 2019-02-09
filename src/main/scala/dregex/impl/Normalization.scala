package dregex.impl

import scala.collection.JavaConverters._

sealed trait Normalization {
  def normalize(s: CharSequence): CharSequence
}

object Normalization {

  case object NoNormalization extends Normalization {
    def normalize(s: CharSequence) = s
  }

  case object LowerCase extends Normalization {
    def normalize(s: CharSequence): String = {
      val builder = new StringBuilder
      for (i <- 0 until s.length) {
        builder.append(AsciiHelper.toLower(s.charAt(i)))
      }
      builder.toString
    }
  }

  case object UnicodeLowerCase extends Normalization {
    def normalize(s: CharSequence): String = {
      val builder = new StringBuilder
      for (codePoint <- s.codePoints.iterator.asScala) {
        builder.append(Character.toLowerCase(codePoint))
      }
      builder.toString
    }
  }

}
