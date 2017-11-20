package com.cybozu.labs.langdetect.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Some character normalization (and exclusion) functionality.
 *
 * This functionality was previously embedded in the NGram class.
 *
 * @author Nakatani Shuyo
 * @author Fabian Kessler
 */
public class CharNormalizer {

    /**
     * Character Normalization (and exclusion).
     * @return Normalized character, the space to exclude the character.
     */
    public static char normalize(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        if (block == Character.UnicodeBlock.BASIC_LATIN) {
            if (ch<'A' || (ch<'a' && ch >'Z') || ch>'z') ch = ' ';
        } else if (block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT) {
            if (LATIN1_EXCLUDED.indexOf(ch)>=0) ch = ' ';
        } else if (block == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            ch = ' ';
        } else if (block == Character.UnicodeBlock.ARABIC) {
            if (ch == '\u06cc') ch = '\u064a';
        } else if (block == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL) {
            if (ch >= '\u1ea0') ch = '\u1ec3';
        } else if (block == Character.UnicodeBlock.HIRAGANA) {
            ch = '\u3042';
        } else if (block == Character.UnicodeBlock.KATAKANA) {
            ch = '\u30a2';
        } else if (block == Character.UnicodeBlock.BOPOMOFO || block == Character.UnicodeBlock.BOPOMOFO_EXTENDED) {
            ch = '\u3105';
        } else if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
            if (cjk_map.containsKey(ch)) ch = cjk_map.get(ch);
        } else if (block == Character.UnicodeBlock.HANGUL_SYLLABLES) {
            ch = '\uac00';
        }
        return ch;
    }

    private static final String LATIN1_EXCLUDED = Messages.getString("NGram.LATIN1_EXCLUDE");
    private static final Map<Character, Character> cjk_map;


    /**
     * CJK Kanji Normalization Mapping
     */
    private static final String[] CJK_CLASS = {
            Messages.getString("NGram.KANJI_1_0"),
            Messages.getString("NGram.KANJI_1_2"),
            Messages.getString("NGram.KANJI_1_4"),
            Messages.getString("NGram.KANJI_1_8"),
            Messages.getString("NGram.KANJI_1_11"),
            Messages.getString("NGram.KANJI_1_12"),
            Messages.getString("NGram.KANJI_1_13"),
            Messages.getString("NGram.KANJI_1_14"),
            Messages.getString("NGram.KANJI_1_16"),
            Messages.getString("NGram.KANJI_1_18"),
            Messages.getString("NGram.KANJI_1_22"),
            Messages.getString("NGram.KANJI_1_27"),
            Messages.getString("NGram.KANJI_1_29"),
            Messages.getString("NGram.KANJI_1_31"),
            Messages.getString("NGram.KANJI_1_35"),
            Messages.getString("NGram.KANJI_2_0"),
            Messages.getString("NGram.KANJI_2_1"),
            Messages.getString("NGram.KANJI_2_4"),
            Messages.getString("NGram.KANJI_2_9"),
            Messages.getString("NGram.KANJI_2_10"),
            Messages.getString("NGram.KANJI_2_11"),
            Messages.getString("NGram.KANJI_2_12"),
            Messages.getString("NGram.KANJI_2_13"),
            Messages.getString("NGram.KANJI_2_15"),
            Messages.getString("NGram.KANJI_2_16"),
            Messages.getString("NGram.KANJI_2_18"),
            Messages.getString("NGram.KANJI_2_21"),
            Messages.getString("NGram.KANJI_2_22"),
            Messages.getString("NGram.KANJI_2_23"),
            Messages.getString("NGram.KANJI_2_28"),
            Messages.getString("NGram.KANJI_2_29"),
            Messages.getString("NGram.KANJI_2_30"),
            Messages.getString("NGram.KANJI_2_31"),
            Messages.getString("NGram.KANJI_2_32"),
            Messages.getString("NGram.KANJI_2_35"),
            Messages.getString("NGram.KANJI_2_36"),
            Messages.getString("NGram.KANJI_2_37"),
            Messages.getString("NGram.KANJI_2_38"),
            Messages.getString("NGram.KANJI_3_1"),
            Messages.getString("NGram.KANJI_3_2"),
            Messages.getString("NGram.KANJI_3_3"),
            Messages.getString("NGram.KANJI_3_4"),
            Messages.getString("NGram.KANJI_3_5"),
            Messages.getString("NGram.KANJI_3_8"),
            Messages.getString("NGram.KANJI_3_9"),
            Messages.getString("NGram.KANJI_3_11"),
            Messages.getString("NGram.KANJI_3_12"),
            Messages.getString("NGram.KANJI_3_13"),
            Messages.getString("NGram.KANJI_3_15"),
            Messages.getString("NGram.KANJI_3_16"),
            Messages.getString("NGram.KANJI_3_18"),
            Messages.getString("NGram.KANJI_3_19"),
            Messages.getString("NGram.KANJI_3_22"),
            Messages.getString("NGram.KANJI_3_23"),
            Messages.getString("NGram.KANJI_3_27"),
            Messages.getString("NGram.KANJI_3_29"),
            Messages.getString("NGram.KANJI_3_30"),
            Messages.getString("NGram.KANJI_3_31"),
            Messages.getString("NGram.KANJI_3_32"),
            Messages.getString("NGram.KANJI_3_35"),
            Messages.getString("NGram.KANJI_3_36"),
            Messages.getString("NGram.KANJI_3_37"),
            Messages.getString("NGram.KANJI_3_38"),
            Messages.getString("NGram.KANJI_4_0"),
            Messages.getString("NGram.KANJI_4_9"),
            Messages.getString("NGram.KANJI_4_10"),
            Messages.getString("NGram.KANJI_4_16"),
            Messages.getString("NGram.KANJI_4_17"),
            Messages.getString("NGram.KANJI_4_18"),
            Messages.getString("NGram.KANJI_4_22"),
            Messages.getString("NGram.KANJI_4_24"),
            Messages.getString("NGram.KANJI_4_28"),
            Messages.getString("NGram.KANJI_4_34"),
            Messages.getString("NGram.KANJI_4_39"),
            Messages.getString("NGram.KANJI_5_10"),
            Messages.getString("NGram.KANJI_5_11"),
            Messages.getString("NGram.KANJI_5_12"),
            Messages.getString("NGram.KANJI_5_13"),
            Messages.getString("NGram.KANJI_5_14"),
            Messages.getString("NGram.KANJI_5_18"),
            Messages.getString("NGram.KANJI_5_26"),
            Messages.getString("NGram.KANJI_5_29"),
            Messages.getString("NGram.KANJI_5_34"),
            Messages.getString("NGram.KANJI_5_39"),
            Messages.getString("NGram.KANJI_6_0"),
            Messages.getString("NGram.KANJI_6_3"),
            Messages.getString("NGram.KANJI_6_9"),
            Messages.getString("NGram.KANJI_6_10"),
            Messages.getString("NGram.KANJI_6_11"),
            Messages.getString("NGram.KANJI_6_12"),
            Messages.getString("NGram.KANJI_6_16"),
            Messages.getString("NGram.KANJI_6_18"),
            Messages.getString("NGram.KANJI_6_20"),
            Messages.getString("NGram.KANJI_6_21"),
            Messages.getString("NGram.KANJI_6_22"),
            Messages.getString("NGram.KANJI_6_23"),
            Messages.getString("NGram.KANJI_6_25"),
            Messages.getString("NGram.KANJI_6_28"),
            Messages.getString("NGram.KANJI_6_29"),
            Messages.getString("NGram.KANJI_6_30"),
            Messages.getString("NGram.KANJI_6_32"),
            Messages.getString("NGram.KANJI_6_34"),
            Messages.getString("NGram.KANJI_6_35"),
            Messages.getString("NGram.KANJI_6_37"),
            Messages.getString("NGram.KANJI_6_39"),
            Messages.getString("NGram.KANJI_7_0"),
            Messages.getString("NGram.KANJI_7_3"),
            Messages.getString("NGram.KANJI_7_6"),
            Messages.getString("NGram.KANJI_7_7"),
            Messages.getString("NGram.KANJI_7_9"),
            Messages.getString("NGram.KANJI_7_11"),
            Messages.getString("NGram.KANJI_7_12"),
            Messages.getString("NGram.KANJI_7_13"),
            Messages.getString("NGram.KANJI_7_16"),
            Messages.getString("NGram.KANJI_7_18"),
            Messages.getString("NGram.KANJI_7_19"),
            Messages.getString("NGram.KANJI_7_20"),
            Messages.getString("NGram.KANJI_7_21"),
            Messages.getString("NGram.KANJI_7_23"),
            Messages.getString("NGram.KANJI_7_25"),
            Messages.getString("NGram.KANJI_7_28"),
            Messages.getString("NGram.KANJI_7_29"),
            Messages.getString("NGram.KANJI_7_32"),
            Messages.getString("NGram.KANJI_7_33"),
            Messages.getString("NGram.KANJI_7_35"),
            Messages.getString("NGram.KANJI_7_37"),
    };
    static {
        cjk_map = new HashMap<>();
        for (String cjk_list : CJK_CLASS) {
            char representative = cjk_list.charAt(0);
            for (int i=0;i<cjk_list.length();++i) {
                cjk_map.put(cjk_list.charAt(i), representative);
            }
        }
    }

}
