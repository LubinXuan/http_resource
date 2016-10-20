package com.adtime.http.resource.detector;

import com.adtime.http.resource.CharsetDetector;
import com.adtime.http.resource.util.CharsetUtils;
import info.monitorenter.cpdetector.io.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public class CPDetector extends CharsetDetector {

    private static CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();

    static {
        detector.add(new ParsingDetector(false));
        detector.add(JChardetFacade.getInstance());
        detector.add(ASCIIDetector.getInstance());
        detector.add(UnicodeDetector.getInstance());
        detector.add(new ByteOrderMarkDetector());
    }

    public String detect(InputStream is, int length) {
        Charset charset = null;
        try {
            charset = detector.detectCodepage(is, length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (null != charset) {
            return charset.displayName();
        } else {
            return null;
        }
    }

    @Override
    public String[] detect(byte[] data, String defaultCharset) {
        String charSet = detect(new ByteArrayInputStream(data), data.length);
        if (CharsetUtils.isValidCharset(charSet)) {
            return new String[]{charSet};
        }
        return null;
    }
}
