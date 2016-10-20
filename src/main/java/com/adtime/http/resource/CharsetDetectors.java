package com.adtime.http.resource;

import com.adtime.http.resource.detector.CPDetector;
import com.adtime.http.resource.detector.HtmlEncodeDetector;
import com.adtime.http.resource.detector.ICUDetector;
import com.adtime.http.resource.detector.NsDetector;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public class CharsetDetectors {

    private static final List<CharsetDetector> DETECTOR_LIST = new ArrayList<>();

    private static final Map<String, Integer> PRIORITY = new HashMap<>();

    private static final Comparator<String> comparator;

    static {
        String detector = System.getProperty("charset.detector", "ICUDetector");
        if ("NsDetector".equals(detector)) {
            DETECTOR_LIST.add(new NsDetector());
            DETECTOR_LIST.add(new ICUDetector());
        } else {
            DETECTOR_LIST.add(new ICUDetector());
            DETECTOR_LIST.add(new NsDetector());
        }
        DETECTOR_LIST.add(new CPDetector());
        DETECTOR_LIST.add(new HtmlEncodeDetector());


        PRIORITY.put("GBK", 0);
        PRIORITY.put("UTF-8", 0);
        PRIORITY.put("GB2312", 1);
        PRIORITY.put("GB18030", 0);
        PRIORITY.put("UTF-16BE", 2);
        PRIORITY.put("UTF-16LE", 2);
        PRIORITY.put("BIG5", 2);
        PRIORITY.put("EUC-JP", 3);
        PRIORITY.put("ASCII", 4);

        comparator = (key1, key2) -> {
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
        };
    }

    public static String[] getCharSet(byte[] data, String defaultCharset) {
        for (CharsetDetector detector : DETECTOR_LIST) {
            String[] propCharset = sort(detector.detect(data, defaultCharset));
            if (null != propCharset) {
                return propCharset;
            }
        }
        return CharsetDetector.DEFAULT;
    }

    private static String[] sort(String[] propCharset) {

        if (null == propCharset || propCharset.length < 1) {
            return null;
        }

        Map<String, Integer> proCharSetSortMap = new TreeMap<>(comparator);

        for (String charSet : propCharset) {
            if (StringUtils.equalsIgnoreCase("void", charSet)) {
                continue;
            }
            try {
                Charset.forName(charSet);
                proCharSetSortMap.put(charSet, PRIORITY.get(charSet.toUpperCase()));
            } catch (Exception ignored) {
            }
        }

        if (proCharSetSortMap.isEmpty()) {
            return null;
        }

        return proCharSetSortMap.keySet().toArray(new String[proCharSetSortMap.size()]);
    }
}
