package com.adtime.http.resource;

import com.adtime.http.resource.exception.InitializeError;
import com.adtime.http.resource.http.*;
import com.adtime.http.resource.url.format.DefaultUrlFormat;
import com.adtime.http.resource.url.format.FormatUrl;
import com.adtime.http.resource.url.invalid.DefaultInvalidUrl;
import com.adtime.http.resource.url.invalid.InvalidUrl;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/11/5.
 */
public class HttpIns {
    private static final FormatUrl FORMATURL = new DefaultUrlFormat();
    private static final InvalidUrl INVALIDURL = new DefaultInvalidUrl();

    private static final CrawlConfig CRAWL_CONFIG = new CrawlConfig();

    private static final HttpClientHelper HTTP_CLIENT_HELPER = new Clients435();

    static {
        CRAWL_CONFIG.setUserAgentString(WebConst.randomUA());
        CRAWL_CONFIG.setIncludeHttpsPages(true);
        HTTP_CLIENT_HELPER.setConfig(CRAWL_CONFIG);
    }

    private static final Map<Object, WebResource> WEB_RESOURCE_MAP = new ConcurrentHashMap<>();

    public static HttpClientResource httpClient() {
        return (HttpClientResource) WEB_RESOURCE_MAP.computeIfAbsent("httpClient", o -> httpClient(CRAWL_CONFIG));
    }

    public static HttpClientResource httpClient(CrawlConfig config) {
        return newInstance(config, HttpClientResource.class, HttpClientHelper.class, HTTP_CLIENT_HELPER);
    }


    public static AsyncHttpClient asyncHttpClient() {
        return (AsyncHttpClient) WEB_RESOURCE_MAP.computeIfAbsent("asyncHttpClient", o -> asyncHttpClient(CRAWL_CONFIG));
    }

    public static AsyncHttpClient asyncHttpClient(CrawlConfig config) {
        return newInstance(config, AsyncHttpClient.class, HttpClientHelper.class, HTTP_CLIENT_HELPER);
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
                Class[] classes = new Class[params.length / 2];
                Object[] param = new Object[params.length / 2];
                for (int i = 0; i < params.length; i += 2) {
                    classes[i / 2] = (Class) params[i];
                    param[i / 2] = params[i + 1];
                }
                Constructor<T> constructor = clazz.getConstructor(classes);
                webResource = constructor.newInstance(param);
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


    private static final Map<CrawlConfig, HttpClient> clientMap = new ConcurrentHashMap<>();

    public static HttpClient global() {
        return global(CRAWL_CONFIG);
    }


    public static HttpClient global(CrawlConfig crawlConfig) {
        return clientMap.computeIfAbsent(crawlConfig, crawlConfig1 -> {
            RequestConfig.Builder builder = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(120000);
            if (StringUtils.isNotBlank(crawlConfig1.getProxyHost())) {
                builder.setProxy(new HttpHost(crawlConfig1.getProxyHost(), crawlConfig1.getProxyPort()));
            }
            RequestConfig config = builder.build();
            HttpClientBuilder clientBuilder = HttpClients.custom().setDefaultRequestConfig(config);
            if (StringUtils.isNotBlank(crawlConfig1.getProxyUsername())) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(new AuthScope(crawlConfig1.getProxyHost(), crawlConfig1.getProxyPort()), new UsernamePasswordCredentials(crawlConfig1.getProxyUsername(), crawlConfig1.getProxyPassword()));
                clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            return clientBuilder.build();
        });
    }

}
