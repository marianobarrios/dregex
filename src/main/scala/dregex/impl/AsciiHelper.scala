package dregex.impl

object AsciiHelper {

  def isUpper(ch: Char): Boolean = {
    ch >= 'A' && ch <= 'Z'
  }

  def toLower(ch: Char): Char = {
    if (isUpper(ch))
      (ch + 0x20).toChar
    else
      ch
  }

}
