package com.adtime.http.resource.util;

import com.adtime.http.resource.WebConst;
import com.adtime.http.resource.url.URLCanonicalizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lubin.Xuan on 2015/12/15.
 */
public class HttpHeaderUtils {

    private final static Map<String, String> httpHeaderTemp;

    static {
        Map<String, String> _httpHeaderTemp = new HashMap<>();
        _httpHeaderTemp.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        //httpHeaderTemp.put("Accept-Charset", "UTF-8,*;q=0.5");
        _httpHeaderTemp.put("Accept-Encoding", "gzip,deflate");
        _httpHeaderTemp.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-TW;q=0.2,ja;q=0.2");
        _httpHeaderTemp.put("Cache-Control", "max-age=0");
        //httpHeaderTemp.put("Connection", "close");
        httpHeaderTemp = Collections.unmodifiableMap(_httpHeaderTemp);
    }

    public static Map<String, String> generateHeaderInfo(Map<String, String> _headers, boolean mobile, String url) {
        Map<String, String> headers;
        if (null != _headers) {
            headers = _headers;
        } else {
            headers = new HashMap<>();
        }
        headers.putAll(httpHeaderTemp);
        if (!headers.containsKey(WebConst.UserAgent)) {
            headers.put(WebConst.UserAgent, WebConst.randomUA(mobile));
        }
        if (!headers.containsKey(WebConst.Referer)) {
            headers.put(WebConst.Referer, url);
        }
        return headers;
    }

    protected static String getReferer(String url) {
        String referer = URLCanonicalizer.getReferer(url);
        if (null != referer && !referer.equals(url) && !(referer + "/").equals(url)) {
            return referer;
        } else {
            return null;
        }
    }
}
