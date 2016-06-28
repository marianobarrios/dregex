package dregex.impl

case class UnicodeChar(codePoint: Int) {
  
  if (!Character.isValidCodePoint(codePoint)) {
    throw new IllegalArgumentException(s"Illegal code point: $codePoint")
  }
  
  override def toString() = {
    /*
     * Avoid array allocation for the very common case of the BMP
     */
    if (Character.charCount(codePoint) == 1) {
        String.valueOf(codePoint.asInstanceOf[Char]);
    } else {
        new String(Character.toChars(codePoint))
    }
  }
  
}
