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

    private static String getMetaVal(String key, String field, Document html) {
        if (null == html) {
            return "";
        }
        Elements meta = html.select("head>meta");
        Optional<Element> elementOptional = meta.parallelStream().filter(m -> key.equalsIgnoreCase(m.attr(field))).findFirst();
        if (elementOptional.isPresent()) {
            return elementOptional.get().attr("content");
        }
        return "";
    }

}
