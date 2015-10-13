package com.adtime.http.resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnicodeUtil {
    private static final Logger logger = LoggerFactory.getLogger(UnicodeUtil.class);

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

    private static String[] lineCtrl = new String[]{"\\/", "\\\\"};
    private static String[] replace = new String[]{"/", "\\"};

    public static String unicode2string(String str) {
        str = (str == null ? "" : str);
        if (!str.contains("\\u"))
            return str;

        int i = next(str, -1);
        while (i > -1) {
            String value = str.substring(i + 2, i + 6);
            try {
                int c = Integer.parseInt(value, 16);
                str = StringUtils.replace(str, "\\u" + value, String.valueOf((char) c));
                i = next(str, i);
            } catch (Exception e) {
                logger.error("{} {}", str, e);
                i = next(str, i + 1);
            }
        }
        return StringUtils.replaceEach(str, lineCtrl, replace);
    }

    private static int next(String str, int pre) {
        int nxt = str.indexOf("\\u", pre);
        if (nxt > -1) {
            int i = nxt + 2;
            int j = 0;
            int f = 4;
            for (; j < 4 && str.length() >= i + 4; j++) {
                char t = str.charAt(i + j);
                if ((t > 47 && t < 58) || (t > 64 && t < 91) || (t > 96 && t < 123)) {
                    f--;
                    continue;
                }
                break;
            }
            if (f == 0) {
                return nxt;
            }
            return next(str, i + j);
        } else {
            return -1;
        }
    }
}
