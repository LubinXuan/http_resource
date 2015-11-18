package com.adtime.http.resource;

import com.adtime.http.resource.url.URLCanonicalizer;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lubin.Xuan on 2015/8/10.
 * ie.
 */
public class RequestBuilder {

    private static final Logger logger = LoggerFactory.getLogger(RequestBuilder.class);

    private final static HashMap<String, String> httpHeaderTemp = new HashMap<>();

    static {
        httpHeaderTemp.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        //httpHeaderTemp.put("Accept-Charset", "UTF-8,*;q=0.5");
        httpHeaderTemp.put("Accept-Encoding", "gzip,deflate");
        httpHeaderTemp.put("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-TW;q=0.2,ja;q=0.2");
        httpHeaderTemp.put("Cache-Control", "max-age=0");
        //httpHeaderTemp.put("Connection", "close");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "30");
        java.security.Security.setProperty("networkaddress.cache.ttl", "300");
        System.setProperty("https.protocols", "TLSv1,SSLv3");
    }

    protected static String getReferer(String url) {
        String referer = URLCanonicalizer.getReferer(url);
        if (null != referer && !referer.equals(url) && !(referer + "/").equals(url)) {
            return referer;
        } else {
            return null;
        }
    }

    private static void allHeaders(Map<String, String> _headers, String url, Request request) {
        Map<String, String> headers = new HashMap<>(httpHeaderTemp);
        if (null != _headers && !_headers.isEmpty()) {
            headers.putAll(_headers);
        }
        if (!headers.containsKey(WebResource.UserAgent)) {
            headers.put(WebResource.UserAgent, WebConst.randomUA(request.isRequestAsMobile()));
        }
        if (!headers.containsKey(WebResource.Referer)) {
           /* String referer = getReferer(url);
            if (null != referer) {
                headers.put(WebResource.Referer, referer);
            }*/
            headers.put(WebResource.Referer, url);
        }
        request.setHeaderMap(headers);
    }

    public static boolean isIndex(String url) {
        String domain = URLCanonicalizer.getDomain(url);
        return url.equals(domain) || url.equals(domain + "/");
    }

    public static Request buildRequest(String url, String charSet, Map<String, String> headers, boolean trust, boolean includeIndex, int maxRedirect) {

        Request request = new Request();

        if (StringUtils.isBlank(url)) {
            request.setCompleted(new Result(url, WebConst.NULL_URL, "文章不能获取 URL为空"));
            return request;
        }

        url = StringEscapeUtils.unescapeHtml4(url);
        if (!includeIndex && isIndex(url)) {
            request.setCompleted(new Result(url, WebConst.LOCAL_INDEX_DENY, "首页 Url: " + url));
            return request;
        }

        String origUrl = url;
        String host;
        try {
            host = new URL(url).getHost();
        } catch (Throwable e) {
            request.setCompleted(new Result(request.requestUrl(), WebConst.LOCAL_HOST_ERROR, "获取host失败 " + url));
            return request;
        }
        String[] part = host.split("\\.");
        int num = 0;
        if (part.length == 4) {
            for (String p : part) {
                try {
                    if (p.length() > 3) {
                        break;
                    } else {
                        int n = Integer.valueOf(p);
                        if (n > -1 && n < 256) {
                            num++;
                        }
                    }
                } catch (Exception e) {
                    break;
                }
            }
        }
        if (num < 4) {
            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                logger.error("域名不可解析", e);
            }
            if (inetAddress == null) {
                request.setCompleted(new Result(request.requestUrl(), WebConst.LOCAL_HOST_ERROR, "域名不可解析 " + host));
                return request;
            }
        }

        request.setCharSet(charSet);
        request.setHeaderMap(headers);
        request.setMaxRedirect(maxRedirect);
        request.setMethod(Request.Method.GET);
        request.setUrl(url);
        request.setTrust(trust);
        allHeaders(headers, url, request);
        request.setOrigUrl(origUrl);
        return request;
    }
}
