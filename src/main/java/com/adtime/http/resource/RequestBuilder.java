package com.adtime.http.resource;

import com.adtime.http.resource.dns.DnsPreFetchUtils;
import com.adtime.http.resource.url.URLCanonicalizer;
import com.adtime.http.resource.util.HttpHeaderUtils;
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

    static {
        System.setProperty("sun.net.spi.nameservice.provider.1", "dns,xbill");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");
        java.security.Security.setProperty("networkaddress.cache.ttl", "1800");
        System.setProperty("https.protocols", "TLSv1,SSLv3");
    }

    private static void allHeaders(Map<String, String> _headers, String url, Request request) {
        Map<String, String> headers = HttpHeaderUtils.generateHeaderInfo(_headers, request.isRequestAsMobile(), url);
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
        DnsPreFetchUtils.preFetch(URLCanonicalizer.getHost(url));
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

    public static Request buildRequest(String url) {
        return buildRequest(url, null, null, true, true, 0);
    }
}
