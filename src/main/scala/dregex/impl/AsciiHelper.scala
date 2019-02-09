package dregex.impl

object AsciiHelper {

  def isUpper(ch: Int) = {
    ch >= 'A' && ch <= 'Z'
  }

  def toLower(ch: Int) = {
    if (isUpper(ch))
      ch + 0x20
    else
      ch
  }

}
