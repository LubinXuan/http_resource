package com.adtime.http.resource.util;

import com.adtime.http.resource.Result;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

/**
 * Created by xuanlubin on 2016/11/8.
 */
public class DocumentUtil {

    private static final Document EMPTY = new Document("");

    public static Document _getDocument(Result result, boolean tidy, boolean asXml) {
        String url = null == result.getMoveToUrl() ? result.getUrl() : result.getMoveToUrl();
        if (null != result.getHtml() && result.getHtml().length() > 0) {
            String html;
            if (tidy) {
                html = JTidyFixHtml.fix(result.getHtml());
            } else {
                html = result.getHtml();
            }

            if (asXml) {
                return Parser.xmlParser().parseInput(html, url);
            } else {
                return Parser.htmlParser().parseInput(html, url);
            }
        } else {
            return EMPTY;
        }
    }

    public static Document getDocument(Result result, boolean asXml) {
        return _getDocument(result, false, asXml);
    }

    public static Document getDocumentTidy(Result result, boolean asXml) {
        return _getDocument(result, true, asXml);
    }
}
