package com.adtime.http.resource;

import com.adtime.http.resource.exception.InitializeError;
import com.adtime.http.resource.http.Clients435;
import com.adtime.http.resource.http.HttpClientResource;
import com.adtime.http.resource.http.HttpUnitResource;
import com.adtime.http.resource.http.HttpUrlConnectionResource;
import com.adtime.http.resource.url.format.DefaultUrlFormat;
import com.adtime.http.resource.url.format.FormatUrl;
import com.adtime.http.resource.url.invalid.DefaultInvalidUrl;
import com.adtime.http.resource.url.invalid.InvalidUrl;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2015/11/5.
 */
public class HttpIns {
    private static final FormatUrl FORMATURL = new DefaultUrlFormat();
    private static final InvalidUrl INVALIDURL = new DefaultInvalidUrl();

    private static final Object HTTP_CLIENT = new Object();
    private static final Object HTML_UNIT = new Object();
    private static final Object HTTP_URL_CONNECTION = new Object();
    private static final CrawlConfig CRAWL_CONFIG = new CrawlConfig();

    static {
        CRAWL_CONFIG.setUserAgentString(WebConst.randomUA());
        CRAWL_CONFIG.setIncludeHttpsPages(true);
    }

    private static final Map<Object, WebResource> WEB_RESOURCE_MAP = new HashMap<>();

    public static WebResource httpClient() {
        if (!WEB_RESOURCE_MAP.containsKey(HTTP_CLIENT)) {
            synchronized (HTTP_CLIENT) {
                if (!WEB_RESOURCE_MAP.containsKey(HTTP_CLIENT)) {
                    WebResource clientResource = httpClient(CRAWL_CONFIG);
                    WEB_RESOURCE_MAP.put(HTTP_CLIENT, clientResource);
                }
            }
        }
        return WEB_RESOURCE_MAP.get(HTTP_CLIENT);
    }

    public static WebResource httpClient(CrawlConfig config) {
        return newInstance(config, HttpClientResource.class, Clients435.class);
    }


    public static WebResource htmluint() {
        if (!WEB_RESOURCE_MAP.containsKey(HTML_UNIT)) {
            synchronized (HTML_UNIT) {
                if (!WEB_RESOURCE_MAP.containsKey(HTML_UNIT)) {
                    WebResource clientResource = httmluint(CRAWL_CONFIG);
                    WEB_RESOURCE_MAP.put(HTML_UNIT, clientResource);
                }
            }
        }
        return WEB_RESOURCE_MAP.get(HTML_UNIT);
    }

    public static WebResource httmluint(CrawlConfig config) {
        return newInstance(config, HttpUnitResource.class);
    }

    public static WebResource httpUrlConnection() {
        if (!WEB_RESOURCE_MAP.containsKey(HTTP_URL_CONNECTION)) {
            synchronized (HTTP_URL_CONNECTION) {
                if (!WEB_RESOURCE_MAP.containsKey(HTTP_URL_CONNECTION)) {
                    WebResource clientResource = httpUrlConnection(CRAWL_CONFIG);
                    WEB_RESOURCE_MAP.put(HTTP_URL_CONNECTION, clientResource);
                }
            }
        }
        return WEB_RESOURCE_MAP.get(HTTP_URL_CONNECTION);
    }

    public static WebResource httpUrlConnection(CrawlConfig config) {
        return newInstance(config, HttpUrlConnectionResource.class);
    }


    private static WebResource newInstance(CrawlConfig config, Class<? extends WebResource> clazz, Object... params) {
        WebResource webResource;
        try {
            if (params.length > 0) {
                Class[] classes = new Class[params.length];
                for (int i = 0; i < params.length; i++) {
                    classes[i] = params[i].getClass();
                }
                Constructor<? extends WebResource> constructor = clazz.getConstructor(classes);
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
