package dregex.impl

object GeneralCategory {

  val categories = Seq(
    ("Cn", Character.UNASSIGNED),
    ("Lu", Character.UPPERCASE_LETTER),
    ("Ll", Character.LOWERCASE_LETTER),
    ("Lt", Character.TITLECASE_LETTER),
    ("Lm", Character.MODIFIER_LETTER),
    ("Lo", Character.OTHER_LETTER),
    ("Mn", Character.NON_SPACING_MARK),
    ("Me", Character.ENCLOSING_MARK),
    ("Mc", Character.COMBINING_SPACING_MARK),
    ("Nd", Character.DECIMAL_DIGIT_NUMBER),
    ("Nl", Character.LETTER_NUMBER),
    ("No", Character.OTHER_NUMBER),
    ("Zs", Character.SPACE_SEPARATOR),
    ("Zl", Character.LINE_SEPARATOR),
    ("Zp", Character.PARAGRAPH_SEPARATOR),
    ("Cc", Character.CONTROL),
    ("Cf", Character.FORMAT),
    ("Co", Character.PRIVATE_USE),
    ("Cs", Character.SURROGATE),
    ("Pd", Character.DASH_PUNCTUATION),
    ("Ps", Character.START_PUNCTUATION),
    ("Pe", Character.END_PUNCTUATION),
    ("Pc", Character.CONNECTOR_PUNCTUATION),
    ("Po", Character.OTHER_PUNCTUATION),
    ("Sm", Character.MATH_SYMBOL),
    ("Sc", Character.CURRENCY_SYMBOL),
    ("Sk", Character.MODIFIER_SYMBOL),
    ("So", Character.OTHER_SYMBOL),
    ("Pi", Character.INITIAL_QUOTE_PUNCTUATION),
    ("Pf", Character.FINAL_QUOTE_PUNCTUATION))
}
