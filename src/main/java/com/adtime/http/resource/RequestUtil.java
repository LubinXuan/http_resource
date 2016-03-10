package com.adtime.http.resource;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Lubin.Xuan on 2015/7/13.
 * ie.
 */
public class RequestUtil {

    public static String buildGetParameter(Map<String, String> requestParam) {
        StringBuilder stringBuilder = new StringBuilder();
        if (null != requestParam && !requestParam.isEmpty()) {
            for (Map.Entry<String, String> entry : requestParam.entrySet()) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("&");
                }
                try {
                    stringBuilder.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "utf-8"));
                } catch (Exception ignore) {
                }
            }
        }
        return stringBuilder.toString();
    }

    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "utf-8");
        } catch (Exception ignore) {
            return "";
        }
    }

}
