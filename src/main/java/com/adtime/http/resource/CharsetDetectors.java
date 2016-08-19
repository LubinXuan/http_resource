package com.adtime.http.resource;

import com.adtime.http.resource.detector.CPDetector;
import com.adtime.http.resource.detector.HtmlEncodeDetector;
import com.adtime.http.resource.detector.ICUDetector;
import com.adtime.http.resource.detector.NsDetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public class CharsetDetectors {

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
