package dregex.impl

import java.io.InputStreamReader
import scala.jdk.CollectionConverters._

object UnicodeDatabase {

  val blocksRanges: Map[String, (Int, Int)] = {
    val blocksFile = classOf[UnicodeDatabaseReader].getResourceAsStream("/Blocks.txt")
    try {
      val blocks = UnicodeDatabaseReader.getBlocks(new InputStreamReader(blocksFile))
      blocks.asScala.mapValues(range => (range.from, range.to)).toMap
    } finally {
      blocksFile.close()
    }
  }

  var blockSynomyms: Map[String, String] = Map(
    "Greek and Coptic" -> "Greek"
  )

  val scriptRanges: Map[String, Seq[(Int, Int)]] = {
    val scriptsFile = classOf[UnicodeDatabaseReader].getResourceAsStream("/Scripts.txt")
    try {
      val scripts = UnicodeDatabaseReader.getScripts(new InputStreamReader(scriptsFile))
      scripts.asScala.mapValues { ranges =>
        ranges.asScala.map(range => (range.from, range.to)).toIndexedSeq
      }.toMap
    } finally {
      scriptsFile.close()
    }
  }

  val scriptSynomyms: Map[String, String] = Map(
    "COMMON" -> "ZYYY",
    "LATIN" -> "LATN",
    "GREEK" -> "GREK",
    "CYRILLIC" -> "CYRL",
    "ARMENIAN" -> "ARMN",
    "HEBREW" -> "HEBR",
    "ARABIC" -> "ARAB",
    "SYRIAC" -> "SYRC",
    "THAANA" -> "THAA",
    "DEVANAGARI" -> "DEVA",
    "BENGALI" -> "BENG",
    "GURMUKHI" -> "GURU",
    "GUJARATI" -> "GUJR",
    "ORIYA" -> "ORYA",
    "TAMIL" -> "TAML",
    "TELUGU" -> "TELU",
    "KANNADA" -> "KNDA",
    "MALAYALAM" -> "MLYM",
    "SINHALA" -> "SINH",
    "THAI" -> "THAI",
    "LAO" -> "LAOO",
    "TIBETAN" -> "TIBT",
    "MYANMAR" -> "MYMR",
    "GEORGIAN" -> "GEOR",
    "HANGUL" -> "HANG",
    "ETHIOPIC" -> "ETHI",
    "CHEROKEE" -> "CHER",
    "CANADIAN_ABORIGINAL" -> "CANS",
    "OGHAM" -> "OGAM",
    "RUNIC" -> "RUNR",
    "KHMER" -> "KHMR",
    "MONGOLIAN" -> "MONG",
    "HIRAGANA" -> "HIRA",
    "KATAKANA" -> "KANA",
    "BOPOMOFO" -> "BOPO",
    "HAN" -> "HANI",
    "YI" -> "YIII",
    "OLD_ITALIC" -> "ITAL",
    "GOTHIC" -> "GOTH",
    "DESERET" -> "DSRT",
    "INHERITED" -> "ZINH",
    "TAGALOG" -> "TGLG",
    "HANUNOO" -> "HANO",
    "BUHID" -> "BUHD",
    "TAGBANWA" -> "TAGB",
    "LIMBU" -> "LIMB",
    "TAI_LE" -> "TALE",
    "LINEAR_B" -> "LINB",
    "UGARITIC" -> "UGAR",
    "SHAVIAN" -> "SHAW",
    "OSMANYA" -> "OSMA",
    "CYPRIOT" -> "CPRT",
    "BRAILLE" -> "BRAI",
    "BUGINESE" -> "BUGI",
    "COPTIC" -> "COPT",
    "NEW_TAI_LUE" -> "TALU",
    "GLAGOLITIC" -> "GLAG",
    "TIFINAGH" -> "TFNG",
    "SYLOTI_NAGRI" -> "SYLO",
    "OLD_PERSIAN" -> "XPEO",
    "KHAROSHTHI" -> "KHAR",
    "BALINESE" -> "BALI",
    "CUNEIFORM" -> "XSUX",
    "PHOENICIAN" -> "PHNX",
    "PHAGS_PA" -> "PHAG",
    "NKO" -> "NKOO",
    "SUNDANESE" -> "SUND",
    "BATAK" -> "BATK",
    "LEPCHA" -> "LEPC",
    "OL_CHIKI" -> "OLCK",
    "VAI" -> "VAII",
    "SAURASHTRA" -> "SAUR",
    "KAYAH_LI" -> "KALI",
    "REJANG" -> "RJNG",
    "LYCIAN" -> "LYCI",
    "CARIAN" -> "CARI",
    "LYDIAN" -> "LYDI",
    "CHAM" -> "CHAM",
    "TAI_THAM" -> "LANA",
    "TAI_VIET" -> "TAVT",
    "AVESTAN" -> "AVST",
    "EGYPTIAN_HIEROGLYPHS" -> "EGYP",
    "SAMARITAN" -> "SAMR",
    "MANDAIC" -> "MAND",
    "LISU" -> "LISU",
    "BAMUM" -> "BAMU",
    "JAVANESE" -> "JAVA",
    "MEETEI_MAYEK" -> "MTEI",
    "IMPERIAL_ARAMAIC" -> "ARMI",
    "OLD_SOUTH_ARABIAN" -> "SARB",
    "INSCRIPTIONAL_PARTHIAN" -> "PRTI",
    "INSCRIPTIONAL_PAHLAVI" -> "PHLI",
    "OLD_TURKIC" -> "ORKH",
    "BRAHMI" -> "BRAH",
    "KAITHI" -> "KTHI",
    "MEROITIC_HIEROGLYPHS" -> "MERO",
    "MEROITIC_CURSIVE" -> "MERC",
    "SORA_SOMPENG" -> "SORA",
    "CHAKMA" -> "CAKM",
    "SHARADA" -> "SHRD",
    "TAKRI" -> "TAKR",
    "MIAO" -> "PLRD",
    "CAUCASIAN_ALBANIAN" -> "AGHB",
    "BASSA_VAH" -> "BASS",
    "DUPLOYAN" -> "DUPL",
    "ELBASAN" -> "ELBA",
    "GRANTHA" -> "GRAN",
    "PAHAWH_HMONG" -> "HMNG",
    "KHOJKI" -> "KHOJ",
    "LINEAR_A" -> "LINA",
    "MAHAJANI" -> "MAHJ",
    "MANICHAEAN" -> "MANI",
    "MENDE_KIKAKUI" -> "MEND",
    "MODI" -> "MODI",
    "MRO" -> "MROO",
    "OLD_NORTH_ARABIAN" -> "NARB",
    "NABATAEAN" -> "NBAT",
    "PALMYRENE" -> "PALM",
    "PAU_CIN_HAU" -> "PAUC",
    "OLD_PERMIC" -> "PERM",
    "PSALTER_PAHLAVI" -> "PHLP",
    "SIDDHAM" -> "SIDD",
    "KHUDAWADI" -> "SIND",
    "TIRHUTA" -> "TIRH",
    "WARANG_CITI" -> "WARA",
    "AHOM" -> "AHOM",
    "ANATOLIAN_HIEROGLYPHS" -> "HLUW",
    "HATRAN" -> "HATR",
    "MULTANI" -> "MULT",
    "OLD_HUNGARIAN" -> "HUNG",
    "SIGNWRITING" -> "SGNW",
    "ADLAM" -> "ADLM",
    "BHAIKSUKI" -> "BHKS",
    "MARCHEN" -> "MARC",
    "NEWA" -> "NEWA",
    "OSAGE" -> "OSGE",
    "TANGUT" -> "TANG",
    "MASARAM_GONDI" -> "GONM",
    "NUSHU" -> "NSHU",
    "SOYOMBO" -> "SOYO",
    "ZANABAZAR_SQUARE" -> "ZANB",
    "HANIFI_ROHINGYA" -> "ROHG",
    "OLD_SOGDIAN" -> "SOGO",
    "SOGDIAN" -> "SOGD",
    "DOGRA" -> "DOGR",
    "GUNJALA_GONDI" -> "GONG",
    "MAKASAR" -> "MAKA",
    "MEDEFAIDRIN" -> "MEDF",
    "ELYMAIC" -> "ELYM",
    "NANDINAGARI" -> "NAND",
    "NYIAKENG_PUACHUE_HMONG" -> "HMNP",
    "WANCHO" -> "WCHO",
    "YEZIDI" -> "YEZI",
    "CHORASMIAN" -> "CHRS",
    "DIVES_AKURU" -> "DIAK",
    "KHITAN_SMALL_SCRIPT" -> "KITS"
  )

}
