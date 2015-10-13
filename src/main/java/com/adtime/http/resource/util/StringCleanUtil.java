package com.adtime.http.resource.util;

public class StringCleanUtil {

    public static String space2one(String str) {
        if (null != str) {
            str = removeInvisible(str).trim();
        }
        if (null != str && !str.trim().isEmpty()) {
            return str.trim().replaceAll("[\\s\\r\\n]{1,1500}", " ");
        } else {
            return str != null ? str.trim() : null;
        }
    }

    public static String removeInvisible(String str) {
        if (null != str) {
            str = str.trim();
            StringBuilder newString = new StringBuilder(str.length());
            for (int offset = 0; offset < str.length(); ) {
                int codePoint = str.codePointAt(offset);
                offset += Character.charCount(codePoint);
                switch (Character.getType(codePoint)) {
                    case Character.CONTROL:     // \p{Cc}
                    case Character.FORMAT:      // \p{Cf}
                    case Character.PRIVATE_USE: // \p{Co}
                    case Character.SURROGATE:   // \p{Cs}
                    case Character.UNASSIGNED:  // \p{Cn}
                    case Character.OTHER_SYMBOL:  // \OTHER_SYMBOL
                    case Character.SPACE_SEPARATOR:  // \SPACE_SEPARATOR
                        newString.append(' ');
                        break;
                    default:
                        newString.append(Character.toChars(codePoint));
                        break;
                }
            }
            return newString.toString();
        } else {
            return "";
        }
    }
}
