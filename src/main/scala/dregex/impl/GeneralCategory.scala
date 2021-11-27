package dregex.impl

object GeneralCategory {

  /**
    * Unicode categories are named in Java using an integer. The mapping
    * is not explicit, so we need to add it here.
    */
  val categories: Map[Byte, String] = {
    import Character._
    Map(
      UNASSIGNED -> "Cn",
      UPPERCASE_LETTER -> "Lu",
      LOWERCASE_LETTER -> "Ll",
      TITLECASE_LETTER -> "Lt",
      MODIFIER_LETTER -> "Lm",
      OTHER_LETTER -> "Lo",
      NON_SPACING_MARK -> "Mn",
      ENCLOSING_MARK -> "Me",
      COMBINING_SPACING_MARK -> "Mc",
      DECIMAL_DIGIT_NUMBER -> "Nd",
      LETTER_NUMBER -> "Nl",
      OTHER_NUMBER -> "No",
      SPACE_SEPARATOR -> "Zs",
      LINE_SEPARATOR -> "Zl",
      PARAGRAPH_SEPARATOR -> "Zp",
      CONTROL -> "Cc",
      FORMAT -> "Cf",
      PRIVATE_USE -> "Co",
      SURROGATE -> "Cs",
      DASH_PUNCTUATION -> "Pd",
      START_PUNCTUATION -> "Ps",
      END_PUNCTUATION -> "Pe",
      CONNECTOR_PUNCTUATION -> "Pc",
      OTHER_PUNCTUATION -> "Po",
      MATH_SYMBOL -> "Sm",
      CURRENCY_SYMBOL -> "Sc",
      MODIFIER_SYMBOL -> "Sk",
      OTHER_SYMBOL -> "So",
      INITIAL_QUOTE_PUNCTUATION -> "Pi",
      FINAL_QUOTE_PUNCTUATION -> "Pf"
    )
  }

  val binaryProperties: Map[String, Int => Boolean] = Map(
    "ALPHABETIC" -> Character.isAlphabetic,
    "DIGIT" -> Character.isDigit,
    "LETTER" -> Character.isLetter,
    "IDEOGRAPHIC" -> Character.isIdeographic,
    "LOWERCASE" -> Character.isLowerCase,
    "UPPERCASE" -> Character.isUpperCase,
    "TITLECASE" -> Character.isTitleCase,
    "WHITE_SPACE" -> isPropertyWhiteSpace,
    "CONTROL" -> isCharacterControl,
    "PUNCTUATION" -> isPropertyPunctuation,
    "HEX_DIGIT" -> isPropertyHexDigit,
    "ASSIGNED" -> ((ch: Int) => Character.getType(ch) != Character.UNASSIGNED),
    "NONCHARACTER_CODE_POINT" -> ((ch: Int) => (ch & 0xfffe) == 0xfffe || (ch >= 0xfdd0 && ch <= 0xfdef)),
    "ALNUM" -> ((ch: Int) => Character.isAlphabetic(ch) || Character.isDigit(ch)),
    "BLANK" -> isPropertyBlank,
    "GRAPH" -> isPropertyGraph,
    "PRINT" ->  ((ch: Int) => (isPropertyGraph(ch) || isPropertyBlank(ch)) && !isCharacterControl(ch)),
    "JOIN_CONTROL" -> isPropertyJoinControl,
    "WORD" -> isPropertyWord
  )

  private def isPropertyWhiteSpace(ch: Int) = {
    val t = Character.getType(ch)
    import Character._
    val isWhiteSpaceCat = t == SPACE_SEPARATOR || t == LINE_SEPARATOR || t == PARAGRAPH_SEPARATOR
    isWhiteSpaceCat || (ch >= 0x9 && ch <= 0xd) || (ch == 0x85)
  }

  private def isCharacterControl(ch: Int) = {
    Character.getType(ch) == Character.CONTROL
  }

  private def isPropertyPunctuation(ch: Int) = {
    val t = Character.getType(ch)
    import Character._
    t == CONNECTOR_PUNCTUATION || t == DASH_PUNCTUATION || t == START_PUNCTUATION || t == END_PUNCTUATION || t == OTHER_PUNCTUATION || t == INITIAL_QUOTE_PUNCTUATION || t == FINAL_QUOTE_PUNCTUATION
  }

  private def isPropertyHexDigit(ch: Int) = {
    Character.isDigit(ch) || (ch >= 0x0030 && ch <= 0x0039) || (ch >= 0x0041 && ch <= 0x0046) || (ch >= 0x0061 && ch <= 0x0066) || (ch >= 0xFF10 && ch <= 0xFF19) || (ch >= 0xFF21 && ch <= 0xFF26) || (ch >= 0xFF41 && ch <= 0xFF46)
  }

  private def isPropertyBlank(ch: Int) = {
    Character.getType(ch) == Character.SPACE_SEPARATOR || ch == 0x9 // \N{HT}
  }

  private def isPropertyGraph(ch: Int) = {
    val t = Character.getType(ch)
    import Character._
    t == SPACE_SEPARATOR || t == LINE_SEPARATOR || t == PARAGRAPH_SEPARATOR || t == CONTROL || t == SURROGATE || t == UNASSIGNED
  }

  private def isPropertyJoinControl(ch: Int) = {
    ch == 0x200C || ch == 0x200D
  }

  private def isPropertyWord(ch: Int) = {
    val t = Character.getType(ch)
    import Character._
    Character.isAlphabetic(ch) || t == NON_SPACING_MARK || t == ENCLOSING_MARK || t == COMBINING_SPACING_MARK || t == DECIMAL_DIGIT_NUMBER || t == CONNECTOR_PUNCTUATION || isPropertyJoinControl(ch)
  }

}
