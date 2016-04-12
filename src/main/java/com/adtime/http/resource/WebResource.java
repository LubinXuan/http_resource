package com.adtime.http.resource;

import com.adtime.http.resource.http.AsyncHttpClient;
import com.adtime.http.resource.proxy.DynamicProxyProvider;
import com.adtime.http.resource.url.URLCanonicalizer;
import com.adtime.http.resource.url.format.FormatUrl;
import com.adtime.http.resource.url.invalid.InvalidUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.function.Consumer;

public abstract class WebResource {

    public static final String UserAgent = WebConst.UserAgent;
    public static final String Referer = WebConst.Referer;

    private static final boolean INDEX_DEFAULT_ACCESS = true;

    protected final static Logger logger = LoggerFactory.getLogger(WebResource.class);

    private InvalidUrl invalidUrl;

    private FormatUrl formatUrl;

    protected CrawlConfig config;

    protected DynamicProxyProvider dynamicProxyProvider;

    static {
        System.setProperty("jsse.enableSNIExtension", "false");
    }

    boolean async = this instanceof AsyncHttpClient;

    public static void enableLite() {
        System.setProperty("icu.lite", "true");
    }

    public Request buildRequest(String url, String charSet, Map<String, String> headers, boolean trust, boolean includeIndex, int maxRedirect) {
        return RequestBuilder.buildRequest(url, charSet, headers, trust, includeIndex, maxRedirect);
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, boolean trust, boolean includeIndex, int maxRedirect) {

        Request checkedRequest = RequestBuilder.buildRequest(url, charSet, headers, trust, includeIndex, maxRedirect);

        return fetchPage(checkedRequest, null);
    }

    public Result fetchPage(Request request) {
        return fetchPage(request, null);
    }

    public Result fetchPage(Request request, Consumer<Result> resultConsumer) {
        Result result = null;
        if (request.isCompleted()) {
            result = request.getResult();
        }

        if (!request.isTrust()) {
            String tmpUrl = validUrl(request.requestUrl());
            if (null == tmpUrl || tmpUrl.trim().length() < 1) {
                request.setCompleted(new Result(request.getOrigUrl(), WebConst.LOCAL_NOT_ACCEPTABLE, "链接406: " + request.getOrigUrl()));
                result = request.getResult();
            }
        }

        if (null != result && null != resultConsumer) {
            resultConsumer.accept(result);
            return result;
        } else {
            return getResult(request, resultConsumer);
        }
    }

    private Result getResult(final Request request, Consumer<Result> resultConsumer) {
        long start = System.currentTimeMillis();
        if (null != resultConsumer && async) {
            AsyncHttpClient asyncHttpClient = (AsyncHttpClient) this;
            asyncHttpClient.async(request.requestUrl(), request.getOrigUrl(), 0, request, result -> {
                if (result.isRedirect() && result.getRedirectCount() < request.getMaxRedirect() && null != result.getMoveToUrl()) {
                    asyncHttpClient.async(result.getMoveToUrl(), result.getMoveToUrl(), result.getRedirectCount(), request, resultConsumer);
                } else if (result.isRedirect()) {
                    result.setMoveToUrl(result.getUrl());
                    result.setUrl(request.getOrigUrl());
                    result.setRequestTime(System.currentTimeMillis() - start);
                    resultConsumer.accept(result);
                } else {
                    result.setRequestTime(System.currentTimeMillis() - start);
                    resultConsumer.accept(result);
                }
            });
            return null;
        } else {
            Result result = request(request.requestUrl(), request.getOrigUrl(), request);
            int redirect = 0;
            while (result.isRedirect() && redirect < request.getMaxRedirect() && null != result.getMoveToUrl()) {
                result = request(result.getMoveToUrl(), result.getMoveToUrl(), request);
                redirect++;
            }

            if (redirect > 0) {
                result.setRedirectCount(redirect);
                if (!result.isRedirect()) {
                    result.setMoveToUrl(result.getUrl());
                }
                result.setUrl(request.getOrigUrl());
            }
            result.setRequestTime(System.currentTimeMillis() - start);
            if (null != resultConsumer) {
                resultConsumer.accept(result);
            }
            return result;
        }
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, boolean trust) {
        return fetchPage(url, charSet, headers, trust, INDEX_DEFAULT_ACCESS, 0);
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers) {
        return fetchPage(url, charSet, headers, INDEX_DEFAULT_ACCESS, 0);
    }

    public Result fetchPage(String url) {
        return fetchPage(url, 0);
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, boolean trust, int maxRedirect) {
        return fetchPage(url, charSet, headers, trust, INDEX_DEFAULT_ACCESS, maxRedirect);
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, int maxRedirect) {
        return fetchPage(url, charSet, headers, INDEX_DEFAULT_ACCESS, maxRedirect);
    }

    public Result fetchPage(String url, int maxRedirect) {
        return fetchPage(url, null, null, false, maxRedirect);
    }


    public abstract Result request(String url, String oUrl, Request request);

    public abstract void registerCookie(String domain, String name, String value);

    protected boolean handException(Throwable e, String url, String oUrl) {
        boolean isTimeOut = e instanceof SocketTimeoutException;
        if (!_handException(e, url, oUrl)) {
            logger.error("", e);
        }

        logger.error("读取页面异常URL:" + url + " - " + oUrl, e);

        return isTimeOut;
    }

    abstract protected boolean _handException(Throwable e, String url, String oUrl);

    protected String validUrl(String url) {
        if (null != formatUrl) {
            url = formatUrl.format(url);
        }
        if (null == url || url.trim().length() < 1) {
            return null;
        } else if (null != invalidUrl && !invalidUrl.valid(url)) {
            return null;
        }
        url = url.replaceAll("(?i)(http://){2,1000}", "http://").replaceAll("%2F", "/").replaceAll("\\\\", "/").replaceAll("\\n|\\t", "").replaceAll("#{2,1000}", "#");
        url = URLCanonicalizer.getCanonicalURL(url);
        if (null == url) {
            return null;
        } else {
            return url.trim();
        }
    }

    protected String metaRefresh(Result result) {
        String refreshUrl = HtmlMetaResolver.getMetaByEquiv("refresh", result.getDocument());
        if (null != refreshUrl && refreshUrl.trim().length() > 0) {
            refreshUrl = refreshUrl.replaceAll(" ", "");
            String url = refreshUrl.toLowerCase();
            if (url.contains(";url=")) {
                int idx = url.indexOf(";url=");
                return refreshUrl.substring(idx + 5);
            }
        }
        return "";
    }

    protected void copy(EntityReadUtils.Entity entity, Result result) {
        result.length = entity.getLength();
        result.unCompressLength = entity.getUnCompressLength();
    }

    public void setFormatUrl(FormatUrl formatUrl) {
        this.formatUrl = formatUrl;
    }

    public void setInvalidUrl(InvalidUrl invalidUrl) {
        this.invalidUrl = invalidUrl;
    }

    public void setConfig(CrawlConfig config) {
        this.config = config;
    }

    public void setDynamicProxyProvider(DynamicProxyProvider dynamicProxyProvider) {
        this.dynamicProxyProvider = dynamicProxyProvider;
    }
}