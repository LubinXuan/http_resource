package com.adtime.http.resource.util;

/**
 * Created by xuanlubin on 2016/8/11.
 */
public class HttpUtil {
    public static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 307;
    }
}
