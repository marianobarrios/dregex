package dregex.impl.database;

import dregex.impl.tree.AbstractRange;
import dregex.impl.tree.CharSet;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnicodeScripts {

    private static final Map<String, List<UnicodeDatabaseReader.Range>> ranges;

    static {
        try (var scriptsFile = UnicodeScripts.class.getResourceAsStream("/Scripts.txt")) {
            ranges = UnicodeDatabaseReader.getScripts(new InputStreamReader(scriptsFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<String, String> synonyms = Map.<String, String>ofEntries(
            Map.entry("COMMON", "ZYYY"),
            Map.entry("LATIN", "LATN"),
            Map.entry("GREEK", "GREK"),
            Map.entry("CYRILLIC", "CYRL"),
            Map.entry("ARMENIAN", "ARMN"),
            Map.entry("HEBREW", "HEBR"),
            Map.entry("ARABIC", "ARAB"),
            Map.entry("SYRIAC", "SYRC"),
            Map.entry("THAANA", "THAA"),
            Map.entry("DEVANAGARI", "DEVA"),
            Map.entry("BENGALI", "BENG"),
            Map.entry("GURMUKHI", "GURU"),
            Map.entry("GUJARATI", "GUJR"),
            Map.entry("ORIYA", "ORYA"),
            Map.entry("TAMIL", "TAML"),
            Map.entry("TELUGU", "TELU"),
            Map.entry("KANNADA", "KNDA"),
            Map.entry("MALAYALAM", "MLYM"),
            Map.entry("SINHALA", "SINH"),
            Map.entry("THAI", "THAI"),
            Map.entry("LAO", "LAOO"),
            Map.entry("TIBETAN", "TIBT"),
            Map.entry("MYANMAR", "MYMR"),
            Map.entry("GEORGIAN", "GEOR"),
            Map.entry("HANGUL", "HANG"),
            Map.entry("ETHIOPIC", "ETHI"),
            Map.entry("CHEROKEE", "CHER"),
            Map.entry("CANADIAN_ABORIGINAL", "CANS"),
            Map.entry("OGHAM", "OGAM"),
            Map.entry("RUNIC", "RUNR"),
            Map.entry("KHMER", "KHMR"),
            Map.entry("MONGOLIAN", "MONG"),
            Map.entry("HIRAGANA", "HIRA"),
            Map.entry("KATAKANA", "KANA"),
            Map.entry("BOPOMOFO", "BOPO"),
            Map.entry("HAN", "HANI"),
            Map.entry("YI", "YIII"),
            Map.entry("OLD_ITALIC", "ITAL"),
            Map.entry("GOTHIC", "GOTH"),
            Map.entry("DESERET", "DSRT"),
            Map.entry("INHERITED", "ZINH"),
            Map.entry("TAGALOG", "TGLG"),
            Map.entry("HANUNOO", "HANO"),
            Map.entry("BUHID", "BUHD"),
            Map.entry("TAGBANWA", "TAGB"),
            Map.entry("LIMBU", "LIMB"),
            Map.entry("TAI_LE", "TALE"),
            Map.entry("LINEAR_B", "LINB"),
            Map.entry("UGARITIC", "UGAR"),
            Map.entry("SHAVIAN", "SHAW"),
            Map.entry("OSMANYA", "OSMA"),
            Map.entry("CYPRIOT", "CPRT"),
            Map.entry("BRAILLE", "BRAI"),
            Map.entry("BUGINESE", "BUGI"),
            Map.entry("COPTIC", "COPT"),
            Map.entry("NEW_TAI_LUE", "TALU"),
            Map.entry("GLAGOLITIC", "GLAG"),
            Map.entry("TIFINAGH", "TFNG"),
            Map.entry("SYLOTI_NAGRI", "SYLO"),
            Map.entry("OLD_PERSIAN", "XPEO"),
            Map.entry("KHAROSHTHI", "KHAR"),
            Map.entry("BALINESE", "BALI"),
            Map.entry("CUNEIFORM", "XSUX"),
            Map.entry("PHOENICIAN", "PHNX"),
            Map.entry("PHAGS_PA", "PHAG"),
            Map.entry("NKO", "NKOO"),
            Map.entry("SUNDANESE", "SUND"),
            Map.entry("BATAK", "BATK"),
            Map.entry("LEPCHA", "LEPC"),
            Map.entry("OL_CHIKI", "OLCK"),
            Map.entry("VAI", "VAII"),
            Map.entry("SAURASHTRA", "SAUR"),
            Map.entry("KAYAH_LI", "KALI"),
            Map.entry("REJANG", "RJNG"),
            Map.entry("LYCIAN", "LYCI"),
            Map.entry("CARIAN", "CARI"),
            Map.entry("LYDIAN", "LYDI"),
            Map.entry("CHAM", "CHAM"),
            Map.entry("TAI_THAM", "LANA"),
            Map.entry("TAI_VIET", "TAVT"),
            Map.entry("AVESTAN", "AVST"),
            Map.entry("EGYPTIAN_HIEROGLYPHS", "EGYP"),
            Map.entry("SAMARITAN", "SAMR"),
            Map.entry("MANDAIC", "MAND"),
            Map.entry("LISU", "LISU"),
            Map.entry("BAMUM", "BAMU"),
            Map.entry("JAVANESE", "JAVA"),
            Map.entry("MEETEI_MAYEK", "MTEI"),
            Map.entry("IMPERIAL_ARAMAIC", "ARMI"),
            Map.entry("OLD_SOUTH_ARABIAN", "SARB"),
            Map.entry("INSCRIPTIONAL_PARTHIAN", "PRTI"),
            Map.entry("INSCRIPTIONAL_PAHLAVI", "PHLI"),
            Map.entry("OLD_TURKIC", "ORKH"),
            Map.entry("BRAHMI", "BRAH"),
            Map.entry("KAITHI", "KTHI"),
            Map.entry("MEROITIC_HIEROGLYPHS", "MERO"),
            Map.entry("MEROITIC_CURSIVE", "MERC"),
            Map.entry("SORA_SOMPENG", "SORA"),
            Map.entry("CHAKMA", "CAKM"),
            Map.entry("SHARADA", "SHRD"),
            Map.entry("TAKRI", "TAKR"),
            Map.entry("MIAO", "PLRD"),
            Map.entry("CAUCASIAN_ALBANIAN", "AGHB"),
            Map.entry("BASSA_VAH", "BASS"),
            Map.entry("DUPLOYAN", "DUPL"),
            Map.entry("ELBASAN", "ELBA"),
            Map.entry("GRANTHA", "GRAN"),
            Map.entry("PAHAWH_HMONG", "HMNG"),
            Map.entry("KHOJKI", "KHOJ"),
            Map.entry("LINEAR_A", "LINA"),
            Map.entry("MAHAJANI", "MAHJ"),
            Map.entry("MANICHAEAN", "MANI"),
            Map.entry("MENDE_KIKAKUI", "MEND"),
            Map.entry("MODI", "MODI"),
            Map.entry("MRO", "MROO"),
            Map.entry("OLD_NORTH_ARABIAN", "NARB"),
            Map.entry("NABATAEAN", "NBAT"),
            Map.entry("PALMYRENE", "PALM"),
            Map.entry("PAU_CIN_HAU", "PAUC"),
            Map.entry("OLD_PERMIC", "PERM"),
            Map.entry("PSALTER_PAHLAVI", "PHLP"),
            Map.entry("SIDDHAM", "SIDD"),
            Map.entry("KHUDAWADI", "SIND"),
            Map.entry("TIRHUTA", "TIRH"),
            Map.entry("WARANG_CITI", "WARA"),
            Map.entry("AHOM", "AHOM"),
            Map.entry("ANATOLIAN_HIEROGLYPHS", "HLUW"),
            Map.entry("HATRAN", "HATR"),
            Map.entry("MULTANI", "MULT"),
            Map.entry("OLD_HUNGARIAN", "HUNG"),
            Map.entry("SIGNWRITING", "SGNW"),
            Map.entry("ADLAM", "ADLM"),
            Map.entry("BHAIKSUKI", "BHKS"),
            Map.entry("MARCHEN", "MARC"),
            Map.entry("NEWA", "NEWA"),
            Map.entry("OSAGE", "OSGE"),
            Map.entry("TANGUT", "TANG"),
            Map.entry("MASARAM_GONDI", "GONM"),
            Map.entry("NUSHU", "NSHU"),
            Map.entry("SOYOMBO", "SOYO"),
            Map.entry("ZANABAZAR_SQUARE", "ZANB"),
            Map.entry("HANIFI_ROHINGYA", "ROHG"),
            Map.entry("OLD_SOGDIAN", "SOGO"),
            Map.entry("SOGDIAN", "SOGD"),
            Map.entry("DOGRA", "DOGR"),
            Map.entry("GUNJALA_GONDI", "GONG"),
            Map.entry("MAKASAR", "MAKA"),
            Map.entry("MEDEFAIDRIN", "MEDF"),
            Map.entry("ELYMAIC", "ELYM"),
            Map.entry("NANDINAGARI", "NAND"),
            Map.entry("NYIAKENG_PUACHUE_HMONG", "HMNP"),
            Map.entry("WANCHO", "WCHO"),
            Map.entry("YEZIDI", "YEZI"),
            Map.entry("CHORASMIAN", "CHRS"),
            Map.entry("DIVES_AKURU", "DIAK"),
            Map.entry("KHITAN_SMALL_SCRIPT", "KITS"));

    public static final Map<String, CharSet> chatSets;

    static {
        Map<String, CharSet> sets = new HashMap<>();
        for (var entry : ranges.entrySet()) {
            var block = entry.getKey();
            var ranges = entry.getValue();
            var chatSet = new CharSet(ranges.stream()
                    .map(range -> AbstractRange.of(range.from, range.to))
                    .collect(Collectors.toList()));
            sets.put(block.toUpperCase(), chatSet);
        }
        for (var entry : synonyms.entrySet()) {
            var script = entry.getKey();
            var alias = entry.getValue();
            sets.put(alias.toUpperCase(), sets.get(script.toUpperCase()));
        }
        chatSets = sets;
    }
}
