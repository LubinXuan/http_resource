package com.adtime.http;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by xuanlubin on 2016/6/24.
 */
public class JFXHtmlUtils {

    private static final Logger logger = LoggerFactory.getLogger(JFXHtmlUtils.class);

    public static String getHtml(Document document) {
        String result = null;

        if (document != null) {
            StringWriter strWtr = new StringWriter();
            StreamResult strResult = new StreamResult(strWtr);
            TransformerFactory tfac = TransformerFactory.newInstance();
            try {
                javax.xml.transform.Transformer t = tfac.newTransformer();
                t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                t.setOutputProperty(OutputKeys.INDENT, "yes");
                t.setOutputProperty(OutputKeys.METHOD, "html"); // xml, html,
                // text
                t.setOutputProperty(
                        "{http://xml.apache.org/xslt}indent-amount", "4");
                t.transform(new DOMSource(document.getDocumentElement()),
                        strResult);
            } catch (Exception e) {
                System.err.println("XML.toString(Document): " + e);
            }
            result = strResult.getWriter().toString();
            try {
                strWtr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static void asFile(Document document, String url) {
        String html = getHtml(document);
        asFile(html, url);
    }

    public static void asFile(String htmlContent, String url) {
        try {
            OutputStream os = new FileOutputStream(URLEncoder.encode(url, "utf-8"));
            IOUtils.write(htmlContent, os);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static final Set<String> FILTER_RES = new HashSet<>();

    public static void init() {
        System.setProperty("java.awt.headless", "false");

        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                if ("http".equals(protocol)) {
                    return new URLFortuneHandler();
                } else return null;
            }
        });

        FILTER_RES.add("jpg");
        FILTER_RES.add("jpeg");
        FILTER_RES.add("gif");
        FILTER_RES.add("png");
        FILTER_RES.add("bmp");
        FILTER_RES.add("tiff");
        FILTER_RES.add("pcx");
        FILTER_RES.add("tga");
        FILTER_RES.add("exif");
        FILTER_RES.add("fpx");
        FILTER_RES.add("svg");
        FILTER_RES.add("psd");
        FILTER_RES.add("cdr");
        FILTER_RES.add("pcd");
        FILTER_RES.add("dxf");
        FILTER_RES.add("ufo");
        FILTER_RES.add("eps");
        FILTER_RES.add("ai");
        FILTER_RES.add("raw");
        FILTER_RES.add("css");
        FILTER_RES.add("mp4");
        FILTER_RES.add("swf");

    }

    private static URLConnection emptyConnection(URL url) {
        return new URLConnection(url) {
            @Override
            public void connect() throws IOException {

            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(new byte[0]);
            }
        };
    }

    private static class URLFortuneHandler extends sun.net.www.protocol.http.Handler {

        protected URLConnection openConnection(URL url) throws IOException {
            String file = url.getFile();
            int mid = file.lastIndexOf(".");
            int e = file.indexOf("?", mid + 1);
            String ext;
            if (e > 0) {
                ext = file.substring(mid + 1, e).toLowerCase();
            } else {
                ext = file.substring(mid + 1, file.length()).toLowerCase();
            }
            if (FILTER_RES.contains(ext.toLowerCase())) {
                return emptyConnection(url);
            } else {
                logger.debug("加载资源:{}", url);
                return super.openConnection(url);
            }
        }
    }
}
