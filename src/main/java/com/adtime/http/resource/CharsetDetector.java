package com.adtime.http.resource;

import com.adtime.http.resource.datector.CPDetector;
import com.adtime.http.resource.datector.HtmlEncodeDetector;
import com.adtime.http.resource.datector.ICUDetector;
import com.adtime.http.resource.datector.NsDetector;

import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public abstract class CharsetDetector {

    public static final Charset UTF_8 = Charset.forName("utf-8");
    public static final Charset GBK = Charset.forName("GBK");

    public static final CharsetInfo DEFAULT = new CharsetInfo(GBK.displayName(), new String[]{});

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

    abstract public CharsetInfo detect(byte[] data, String defaultCharset);

    public static CharsetInfo getCharSet(byte[] data, String defaultCharset) {
        for (CharsetDetector detector : DETECTOR_LIST) {
            CharsetInfo charsetInfo = detector.detect(data, defaultCharset);
            if (null != charsetInfo) {
                return charsetInfo;
            }
        }
        return DEFAULT;
    }

    static public class CharsetInfo {
        private String charset;
        private String[] propCharset;

        public CharsetInfo(String charset, String[] propCharset) {
            this.charset = charset;
            this.propCharset = propCharset;
        }

        public String getCharset() {
            return charset;
        }

        public String[] getPropCharset() {
            return propCharset;
        }
    }
}
