package com.adtime.http.resource.detector;

import com.adtime.http.resource.CharsetDetector;
import com.adtime.http.resource.util.CharsetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.Charset;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public class HtmlEncodeDetector extends CharsetDetector {
    @Override
    public CharsetInfo detect(byte[] data, String defaultCharset) {
        String charSet = generateCharSet(new String(data, UTF_8), defaultCharset);
        if (CharsetUtils.isValidCharset(charSet)) {
            return new CharsetInfo(charSet, new String[]{});
        }
        return null;
    }

    public String generateCharSet(String origHtml, String dCharSet) {
        String charSet = null;

        Document document = Jsoup.parse(origHtml);
        Elements metaList = document.select("meta");

        if (!metaList.isEmpty()) {
            for (Element element : metaList) {
                charSet = element.attr("charset");
                if (null == charSet || charSet.trim().length() < 1) {
                    if ("Content-Type".equalsIgnoreCase(element.attr("http-equiv"))) {
                        String charsetContent = element.attr("content");
                        if (null != charsetContent && charsetContent.trim().length() > 0) {
                            if (charsetContent.toLowerCase().contains("x-gbk")) {
                                charSet = "gbk";
                            } else {
                                try {
                                    ContentType contentType = ContentType.parse(charsetContent);
                                    charSet = contentType.getCharset().displayName();
                                } catch (Exception ignore) {
                                }
                            }
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
        }

        if ("x-gbk".equalsIgnoreCase(charSet)) {
            charSet = "gbk";
        }

        if (null != charSet && charSet.trim().length() != 0) {
            try {
                Charset.forName(charSet);
            } catch (Exception e) {
                charSet = null;
            }
        }
        return StringUtils.isNotBlank(charSet) ? charSet : null != dCharSet ? dCharSet : GBK.displayName();
    }
}
