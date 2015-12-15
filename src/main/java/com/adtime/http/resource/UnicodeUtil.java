package com.adtime.http.resource;

import com.adtime.http.resource.util.StringCleanUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnicodeUtil {
    private static final Logger logger = LoggerFactory.getLogger(UnicodeUtil.class);

    public static String removeInvisible(String str) {
        return StringCleanUtil.removeInvisible(str);
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
