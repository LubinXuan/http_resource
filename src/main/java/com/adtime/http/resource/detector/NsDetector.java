package com.adtime.http.resource.detector;

import com.adtime.http.resource.CharsetDetector;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.intl.chardet.HtmlCharsetDetector;
import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsICharsetDetectionObserver;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public class NsDetector extends CharsetDetector {
    private static final Map<String, Integer> PRIORITY = new HashMap<>();

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
    public String [] detect(byte[] data, String defaultCharset) {
        nsDetector det = readEncoding(data);

        String[] proCharSet;

        if (null != det) {
            proCharSet = det.getProbableCharsets();
        } else {
            proCharSet = new String[]{"ASCII"};
        }

        return proCharSet;
    }
}
