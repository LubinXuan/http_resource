package com.adtime.http.resource;

import java.nio.charset.Charset;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public abstract class CharsetDetector {

    public static final Charset UTF_8 = Charset.forName("utf-8");
    public static final Charset GBK = Charset.forName("GBK");

    public static final String[] DEFAULT = new String[]{GBK.displayName()};

    abstract public String[] detect(byte[] data, String defaultCharset);
}
