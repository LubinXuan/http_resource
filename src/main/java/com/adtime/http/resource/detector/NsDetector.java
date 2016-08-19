package com.adtime.http.resource.detector;

import com.adtime.http.resource.CharsetDetector;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.intl.chardet.HtmlCharsetDetector;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public class NsDetector extends CharsetDetector {
    private final Map<String, Integer> PRIORITY = new HashMap<>();

    {
        PRIORITY.put("GBK", 0);
        PRIORITY.put("UTF-8", 0);
        PRIORITY.put("GB2312", 1);
        PRIORITY.put("GB18030", 0);
        PRIORITY.put("UTF-16BE", 2);
        PRIORITY.put("UTF-16LE", 2);
        PRIORITY.put("BIG5", 2);
        PRIORITY.put("EUC-JP", 3);
    }

    private nsDetector readEncoding(byte[] bytes) {
        nsDetector det = new nsDetector();
        nsICharsetDetectionObserver cdo = charset -> HtmlCharsetDetector.found = true;
        det.Init(cdo);
        boolean isAscii = det.isAscii(bytes, bytes.length);
        if (!isAscii) {
            det.DoIt(bytes, bytes.length, false);
            det.DataEnd();
            return det;
        } else {
            return null;
        }
    }

    @Override
    public CharsetInfo detect(byte[] data, String defaultCharset) {
        nsDetector det = readEncoding(data);

        String charSet = null;

        String[] proCharSet;

        if (null != det) {
            proCharSet = det.getProbableCharsets();
        } else {
            proCharSet = new String[]{"ASCII"};
        }

        Map<String, Integer> proCharSetSortMap = new TreeMap<>((key1, key2) -> {
            Integer idx1 = PRIORITY.get(StringUtils.upperCase(key1));
            Integer idx2 = PRIORITY.get(StringUtils.upperCase(key2));
            if ((null == idx1 && null == idx2)) {
                return 0;
            } else if (null == idx1) {
                return 1;
            } else if (null == idx2) {
                return -1;
            } else if (idx1 > idx2) {
                return 1;
            } else if (idx2 > idx1) {
                return -1;
            } else {
                return 0;
            }
        });

        if (null != proCharSet && proCharSet.length > 0) {
            for (String p : proCharSet) {
                proCharSetSortMap.put(p, PRIORITY.get(p.toUpperCase()));
            }
        }

        for (String c : proCharSetSortMap.keySet()) {
            try {
                Charset c_s = Charset.forName(c);
                charSet = c_s.displayName();
                if ("void".equalsIgnoreCase(charSet)) {
                    charSet = null;
                    continue;
                }
                break;
            } catch (Exception ignored) {

            }
        }
        if (null != charSet && charSet.toLowerCase().startsWith("utf")) {
            charSet = UTF_8.displayName();
        }

        if (null != charSet) {
            return new CharsetInfo(charSet, proCharSet);
        } else {
            return null;
        }
    }
}
