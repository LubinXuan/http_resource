package com.adtime.http.resource.util;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;

/**
 * Created by xuanlubin on 2016/6/7.
 */
public class CharsetUtils {
    public static boolean isValidCharset(String charset) {
        if (StringUtils.isBlank(charset)) {
            return false;
        } else {
            try {
                Charset.forName(charset);
                return true;
            } catch (Throwable e) {
                return false;
            }
        }
    }
}
