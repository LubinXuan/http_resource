package com.adtime.http.resource;

import com.adtime.http.resource.exception.InitializeError;
import com.adtime.http.resource.http.*;
import com.adtime.http.resource.url.format.DefaultUrlFormat;
import com.adtime.http.resource.url.format.FormatUrl;
import com.adtime.http.resource.url.invalid.DefaultInvalidUrl;
import com.adtime.http.resource.url.invalid.InvalidUrl;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by Administrator on 2015/11/5.
 */
public class HttpIns {
    private static final FormatUrl FORMATURL = new DefaultUrlFormat();
    private static final InvalidUrl INVALIDURL = new DefaultInvalidUrl();

    private static final CrawlConfig CRAWL_CONFIG = new CrawlConfig();

    static {
        CRAWL_CONFIG.setUserAgentString(WebConst.randomUA());
        CRAWL_CONFIG.setIncludeHttpsPages(true);
    }

    private static final Map<Object, WebResource> WEB_RESOURCE_MAP = new ConcurrentHashMap<>();

    public static HttpClientResource httpClient() {
        return (HttpClientResource) WEB_RESOURCE_MAP.computeIfAbsent("httpClient", o -> httpClient(CRAWL_CONFIG));
    }

    public static HttpClientResource httpClient(CrawlConfig config) {
        return newInstance(config, HttpClientResource.class, Clients435.class);
    }


    public static AsyncHttpClient asyncHttpClient() {
        return (AsyncHttpClient) WEB_RESOURCE_MAP.computeIfAbsent("asyncHttpClient", o -> httmluint(CRAWL_CONFIG));
    }

    public static AsyncHttpClient asyncHttpClient(CrawlConfig config) {
        return newInstance(config, AsyncHttpClient.class);
    }

    public static HttpUnitResource htmluint() {
        return (HttpUnitResource) WEB_RESOURCE_MAP.computeIfAbsent("htmluint", o -> httmluint(CRAWL_CONFIG));
    }

    public static HttpUnitResource httmluint(CrawlConfig config) {
        return newInstance(config, HttpUnitResource.class);
    }

    public static HttpUrlConnectionResource httpUrlConnection() {
        return (HttpUrlConnectionResource) WEB_RESOURCE_MAP.computeIfAbsent("httpUrlConnection", o -> httpUrlConnection(CRAWL_CONFIG));
    }

    public static HttpUrlConnectionResource httpUrlConnection(CrawlConfig config) {
        return newInstance(config, HttpUrlConnectionResource.class);
    }


    private static <T extends WebResource> T newInstance(CrawlConfig config, Class<T> clazz, Object... params) {
        T webResource;
        try {
            if (params.length > 0) {
                Class[] classes = new Class[params.length];
                for (int i = 0; i < params.length; i++) {
                    classes[i] = params[i].getClass();
                }
                Constructor<T> constructor = clazz.getConstructor(classes);
                webResource = constructor.newInstance(params);
            } else {
                webResource = clazz.newInstance();
            }
        } catch (Exception e) {
            throw new InitializeError("http 组件初始化失败", e);
        }
        webResource.setConfig(config);
        webResource.setFormatUrl(FORMATURL);
        webResource.setInvalidUrl(INVALIDURL);
        return webResource;
    }
}
