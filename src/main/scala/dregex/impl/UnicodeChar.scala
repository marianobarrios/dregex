package dregex.impl

case class UnicodeChar(codePoint: Int) extends Ordered[UnicodeChar] {

  if (!Character.isValidCodePoint(codePoint)) {
    throw new IllegalArgumentException(s"Illegal code point: $codePoint")
  }

  def compare(that: UnicodeChar): Int = this.codePoint compare that.codePoint

  /**
   * Only for debugging
   */
  override def toString() = {
    if (Character.isLetterOrDigit(codePoint))
      s"‘${new String(Character.toChars(codePoint))}’"
    else
      f"$codePoint%X₁₆"
  }

  def +(that: Int): UnicodeChar = UnicodeChar(codePoint + that)
  def -(that: Int): UnicodeChar = UnicodeChar(codePoint - that)

  def toRegex = {
    if (Character.isLetterOrDigit(codePoint))
      new String(Character.toChars(codePoint))
    else
      f"\\x{$codePoint%X}"
  }

  def toJavaString = new String(Character.toChars(codePoint))
  
}

object UnicodeChar {

  def fromChar(char: Char) = {
    UnicodeChar(char)
  }

  def fromSingletonString(str: String) = {
    if (Character.codePointCount(str, 0, str.size) > 1)
      throw new IllegalAccessException("String is no char: " + str)
    UnicodeChar(Character.codePointAt(str, 0))
  }

  val min = UnicodeChar(Character.MIN_CODE_POINT)
  val max = UnicodeChar(Character.MAX_CODE_POINT)

  implicit class FromCharConversion(val char: Char) extends AnyVal {
    def u = UnicodeChar.fromChar(char)
  }

  implicit class FromIntConversion(val int: Int) extends AnyVal {
    def u = UnicodeChar(int)
  }

}