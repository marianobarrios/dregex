package dregex.impl;

public class AsciiHelper {

  public static boolean isUpper(char ch) {
      return ch >= 'A' && ch <= 'Z';
  }

  public static char toLower(char ch) {
      return isUpper(ch) ? (char) (ch + 0x20) : ch;
  }
}
