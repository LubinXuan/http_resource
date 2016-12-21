package com.adtime.http.resource;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Optional;

/**
 * Created by Lubin.Xuan on 2014/11/27.
 * HTML 头部meta信息提取
 */
public class HtmlMetaResolver {
    public static String getMetaByName(String key, Document html) {
        return getMetaVal(key, "name", html);
    }

    public static String getMetaByEquiv(String key, Document html) {
        return getMetaVal(key, "http-equiv", html);
    }

    private static String getMetaVal(String value, String field, Document html) {
        if (null == html) {
            return "";
        }
        Elements meta = html.select("head>meta[" + field + "=" + value + "]");
        if (!meta.isEmpty()) {
            return meta.attr("content");
        }
        return "";
    }

}
