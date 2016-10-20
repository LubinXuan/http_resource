package com.adtime.http.resource.http;

import com.adtime.http.resource.*;
import com.adtime.http.resource.extend.DynamicProxyHttpRoutePlanner;
import com.adtime.http.resource.extend.DynamicProxySelector;
import com.adtime.http.resource.http.htmlunit.HttpWebConnectionWrap;
import com.adtime.http.resource.url.URLCanonicalizer;
import com.adtime.http.resource.util.HttpUtil;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.gae.GAEUtils;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.gargoylesoftware.htmlunit.util.UrlUtils;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;

import javax.annotation.PostConstruct;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class HttpUnitResource extends WebResource {

   /* static {
        //System.setProperty("com.google.appengine.runtime.environment", "true");
    }*/

    private static final CookieManager cookieManager = new HttpUnitCookieManager();

    private HttpRoutePlanner routePlanner;

    @PostConstruct
    public void _init() {
        if (null != dynamicProxyProvider) {
            if (GAEUtils.isGaeMode()) {
                ProxySelector.setDefault(new DynamicProxySelector(dynamicProxyProvider));
            } else {
                this.routePlanner = new DynamicProxyHttpRoutePlanner(new DefaultSchemePortResolver(), dynamicProxyProvider);
            }
        }
    }

    public WebClient build(CrawlConfig config, Request request) {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        if (null != request.getConnectionTimeout()) {
            webClient.getOptions().setTimeout(request.getConnectionTimeout());
        } else {
            webClient.getOptions().setTimeout(config.getConnectionTimeout());
        }
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setAppletEnabled(false);
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(true);
        if (null != config.getProxyHost()) {
            webClient.getOptions().setProxyConfig(new ProxyConfig(config.getProxyHost(), config.getProxyPort()));
            if (null != config.getProxyUsername()) {
                DefaultCredentialsProvider credentialsProvider = new DefaultCredentialsProvider();
                credentialsProvider.addCredentials(config.getProxyUsername(), config.getProxyPassword());
                webClient.setCredentialsProvider(credentialsProvider);
            }
        }
        webClient.setCookieManager(cookieManager);
        WebConnection connection = webClient.getWebConnection();
        if (connection instanceof HttpWebConnection) {
            HttpWebConnectionWrap wrap = new HttpWebConnectionWrap((HttpWebConnection) connection, this.routePlanner);
            webClient.setWebConnection(wrap);
        }
        return webClient;
    }

    @Override
    public void registerCookie(String domain, String name, String value) {
        cookieManager.addCookie(new Cookie(HttpClientHelper.newClientCookie(domain, name, value)));
    }

    @Override
    public Result request(String url, String oUrl, Request request) {
        WebClient webClient = build(config, request);
        try {
            Map<String, String> _headers = request.getHeaderMap();
            String domain = URLCanonicalizer.getHost(url);
            _headers.forEach((k, v) -> {
                if (null != v) {
                    if (!"cookie".equalsIgnoreCase(k)) {
                        webClient.addRequestHeader(k, v);
                    } else if (null != domain) {
                        String[] cookiePair = v.split(";");
                        for (String onePair : cookiePair) {
                            String pair[] = onePair.split("=");
                            if (pair.length >= 2) {
                                registerCookie(domain, pair[0], pair[1]);
                            }
                        }
                    }
                }
            });
            WebRequest webRequest;
            webClient.addRequestHeader("Connection", "close");
            if (Request.Method.GET.equals(request.getMethod()) || Request.Method.HEAD.equals(request.getMethod())) {
                webRequest = new WebRequest(UrlUtils.toUrlUnsafe(url), HttpMethod.valueOf(request.getMethod().name()));
            } else {
                webRequest = new WebRequest(UrlUtils.toUrlUnsafe(url), HttpMethod.valueOf(request.getMethod().name()));
                if (null != request.getRequestParam() && !request.getRequestParam().isEmpty()) {
                    List<NameValuePair> valuePairs = new ArrayList<>();
                    for (Map.Entry<String, String> entry : request.getRequestParam().entrySet()) {
                        valuePairs.add(new NameValuePair(entry.getKey(), URLEncoder.encode(entry.getValue(), "utf-8")));
                    }
                    webRequest.setRequestParameters(valuePairs);
                }
            }
            if (request.getMaxRedirect() < 1) {
                webClient.getOptions().setRedirectEnabled(false);
            }
            com.gargoylesoftware.htmlunit.Page page = webClient.getPage(webRequest);

            Map<String, List<String>> headerMap = null;

            if (request.isReturnHeader() && null != page.getWebResponse().getResponseHeaders()) {
                List<NameValuePair> headers = page.getWebResponse().getResponseHeaders();
                headerMap = new HashMap<>();
                for (NameValuePair header : headers) {
                    headerMap.compute(header.getName(), (s, strings) -> {
                        if (null == strings) {
                            strings = new ArrayList<>();
                        }
                        if (!strings.contains(header.getValue())) {
                            strings.add(header.getValue());
                        }
                        return strings;
                    });
                }
            }

            int sts = page.getWebResponse().getStatusCode();
            Result result;

            if (HttpUtil.isRedirect(sts)) {
                result = new Result(url, page.getWebResponse().getContentAsString(), true, sts);
                result.setMoveToUrl(page.getWebResponse().getResponseHeaderValue("location"));
            } else {
                if (Request.Method.HEAD.equals(request.getMethod())) {
                    result = new Result(url, sts, "").withHeader(headerMap);
                } else {
                    result = new Result(url, page.getWebResponse().getContentAsString(), false, sts);
                }
            }
            return result.withHeader(headerMap);
        } catch (RuntimeException e) {
            handException(e, url, oUrl);
            return new Result(oUrl, WebConst.HTTP_ERROR, e.toString());
        } catch (Exception e) {
            handException(e, url, oUrl);
            return new Result(oUrl, WebConst.HTTP_ERROR, e.toString());
        } finally {
            webClient.close();
        }
    }

    @Override
    protected boolean _handException(Throwable e, String url, String oUrl) {
        return e instanceof SocketException ||
                e instanceof SocketTimeoutException ||
                e instanceof UnknownHostException;
    }
}
