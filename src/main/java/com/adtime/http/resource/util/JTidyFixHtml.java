package com.adtime.http.resource.util;

import org.w3c.tidy.Tidy;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

/**
 * Created by xuanlubin on 2016/7/7.
 */
public class JTidyFixHtml {

    private static final Tidy TIDY = new Tidy();

    static {
        TIDY.setXHTML(true); // 输出格式 xml
        TIDY.setHideComments(true);
        TIDY.setDropEmptyParas(true);
        TIDY.setMakeClean(true); // 删除混乱的表示
        TIDY.setPrintBodyOnly(true);
        TIDY.setIndentAttributes(false);
        TIDY.setShowWarnings(false); // 不显示警告信息
        TIDY.setInputEncoding("UTF-8"); // 输入的流的编码为utf-8
        TIDY.setOutputEncoding("UTF-8"); // 输出流的编码为ｕｔｆ－８
    }

    public static String fix(String html) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.w3c.dom.Document doc = TIDY.parseDOM(new StringReader(html), null);
        TIDY.pprint(doc, out);
        return new String(out.toByteArray());
    }
}
