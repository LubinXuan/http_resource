package com.adtime.http.resource.http;

import com.adtime.http.resource.*;
import com.adtime.http.resource.extend.DynamicProxyHttpRoutePlanner;
import com.adtime.http.resource.extend.DynamicProxySelector;
import com.adtime.http.resource.http.htmlunit.HttpWebConnectionWrap;
import com.adtime.http.resource.url.URLInetAddress;
import com.adtime.http.resource.util.HttpUtil;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.gae.GAEUtils;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.commons.lang3.StringUtils;
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
        WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
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
        webClient.getOptions().setRedirectEnabled(false);
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
    public void clearAllCookie() {
        cookieManager.clearCookies();
    }

    @Override
    public Result request(String url, String oUrl, Request request) {

        URL __url;

        try {
            __url = URLInetAddress.create(url);
        } catch (Exception e) {
            handException(e, null, url, oUrl);
            return new Result(url, WebConst.HTTP_ERROR, e.toString());
        }

        WebClient webClient = build(config, request);
        try {

            String cookie_host = __url.getHost();

            WebRequest webRequest;

            if (Request.Method.GET.equals(request.getMethod()) || Request.Method.HEAD.equals(request.getMethod())) {
                webRequest = new WebRequest(__url, HttpMethod.valueOf(request.getMethod().name()));
            } else {
                webRequest = new WebRequest(__url, HttpMethod.valueOf(request.getMethod().name()));
                if (null != request.getRequestParam() && !request.getRequestParam().isEmpty()) {
                    List<NameValuePair> valuePairs = new ArrayList<>();
                    for (Map.Entry<String, String> entry : request.getRequestParam().entrySet()) {
                        valuePairs.add(new NameValuePair(entry.getKey(), URLEncoder.encode(entry.getValue(), "utf-8")));
                    }
                    webRequest.setRequestParameters(valuePairs);
                }
            }

            webRequest.setAdditionalHeader("Host", cookie_host);
            webRequest.setAdditionalHeader("Connection", "close");

            Map<String, String> _headers = request.getHeaderMap();
            for (Map.Entry<String, String> entry : _headers.entrySet()) {
                if (StringUtils.equalsIgnoreCase(WebConst.COOKIE, entry.getKey())) {
                    String[] cookiePair = entry.getValue().split(";");
                    for (String onePair : cookiePair) {
                        String pair[] = onePair.split("=");
                        if (pair.length >= 2) {
                            registerCookie(cookie_host, pair[0], pair[1]);
                        }
                    }
                } else if (StringUtils.equalsIgnoreCase(WebConst.UserAgent, entry.getKey())) {
                    webClient.getBrowserVersion().setUserAgent(entry.getValue());
                } else {
                    webRequest.setAdditionalHeader(entry.getKey(), entry.getValue());
                }
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
                    result = getResult(page.getWebResponse(), request.getCharSet(), url, true);
                }
            }
            return result.withHeader(headerMap);
        } catch (Throwable e) {
            handException(e, __url.getAuthority(), url, oUrl);
            return new Result(oUrl, WebConst.HTTP_ERROR, e.toString());
        } finally {
            webClient.close();
        }
    }

    private Result getResult(WebResponse webResponse, String charSet, String url, boolean checkBodySize) throws Exception {
        String header_contentType = webResponse.getContentType();
        if (null != header_contentType) {
            header_contentType = header_contentType.replaceAll("\\s", "");
        }
        String contentType = null;
        String contentCharset = null;
        if (null != header_contentType) {
            String[] part = StringUtils.splitByWholeSeparator(header_contentType.toLowerCase(), ";charset=");
            if (part.length == 2) {
                contentType = part[0];
                contentCharset = part[1];
            } else {
                contentType = header_contentType;
            }
        }

        if (null == charSet && null != contentCharset) {
            charSet = contentCharset;
        }

        EntityReadUtils.Entity entity = EntityReadUtils.read(webResponse, charSet, checkBodySize);
        String content = entity.toString(url);
        Result tmp = new Result(url, content, false, webResponse.getStatusCode()).setContentType(contentType);
        copy(entity, tmp);
        entity = null;
        return tmp;
    }


    @Override
    protected boolean _handException(Throwable e, String url, String oUrl) {
        return e instanceof SocketException ||
                e instanceof SocketTimeoutException ||
                e instanceof UnknownHostException;
    }
}
