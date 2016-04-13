package com.adtime.http.resource;

import com.adtime.http.resource.datector.CPDetector;
import com.adtime.http.resource.datector.HtmlEncodeDetector;
import com.adtime.http.resource.datector.ICUDetector;
import com.adtime.http.resource.datector.NsDetector;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public abstract class CharsetDetectorUtil {

    private static final List<CharsetDetector> DETECTOR_LIST = new ArrayList<>();

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
    }

    public static boolean validCharset(String charSet) {
        return !(null == charSet || "void".equalsIgnoreCase(charSet));
    }

    abstract public CharsetDetector.CharsetInfo detect(byte[] data, String defaultCharset);

    public static CharsetDetector.CharsetInfo getCharSet(byte[] data, String defaultCharset) {
        for (CharsetDetector detector : DETECTOR_LIST) {
            CharsetDetector.CharsetInfo charsetInfo = detector.detect(data, defaultCharset);
            if (null != charsetInfo) {
                return charsetInfo;
            }
        }
        return CharsetDetector.DEFAULT;
    }
}
