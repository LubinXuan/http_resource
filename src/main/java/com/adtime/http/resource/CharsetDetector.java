package com.adtime.http.resource;

import java.nio.charset.Charset;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public abstract class CharsetDetector {

    public static final Charset UTF_8 = Charset.forName("utf-8");
    public static final Charset GBK = Charset.forName("GBK");

    public static final CharsetInfo DEFAULT = new CharsetInfo(GBK.displayName(), new String[]{});

    public static boolean validCharset(String charSet) {
        return !(null == charSet || "void".equalsIgnoreCase(charSet));
    }

    abstract public CharsetInfo detect(byte[] data, String defaultCharset);

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
