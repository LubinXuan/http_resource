package com.adtime.http.resource;

import com.adtime.http.resource.connection.HttpRetryHandler;
import com.adtime.http.resource.http.AsyncHttpClient;
import com.adtime.http.resource.proxy.DynamicProxyProvider;
import com.adtime.http.resource.url.URLCanonicalizer;
import com.adtime.http.resource.url.format.FormatUrl;
import com.adtime.http.resource.url.invalid.InvalidUrl;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class WebResource {

    public static final String UserAgent = WebConst.UserAgent;
    public static final String Referer = WebConst.Referer;

    private static final boolean INDEX_DEFAULT_ACCESS = true;

    protected final static Logger logger = LoggerFactory.getLogger(WebResource.class);

    private final static Set<HttpRetryHandler> HTTP_RETRY_HANDLER_SET = new HashSet<>();

    private InvalidUrl invalidUrl;

    private FormatUrl formatUrl;

    protected CrawlConfig config;

    protected DynamicProxyProvider dynamicProxyProvider;

    protected Set<String> cookieDisableHost = new HashSet<>();

    static {
        System.setProperty("jsse.enableSNIExtension", "false");
        if (StringUtils.equalsIgnoreCase("multicast", System.getProperty("networkMonitor"))) {
            ConnectionAbortUtils.init(NetworkMonitor::multicastMonitor);
        } else {
            ConnectionAbortUtils.init(NetworkMonitor::fileMonitor);
        }
    }

    private boolean async = this instanceof AsyncHttpClient;

    public static void addHttpRetryHandler(HttpRetryHandler httpRetryHandler) {
        HTTP_RETRY_HANDLER_SET.add(httpRetryHandler);
    }

    public static void enableLite() {
        System.setProperty("icu.lite", "true");
    }

    public Request buildRequest(String url, String charSet, Map<String, String> headers, boolean trust, boolean includeIndex, int maxRedirect) {
        return RequestBuilder.buildRequest(url, charSet, headers, trust, includeIndex, maxRedirect);
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, boolean trust, boolean includeIndex, int maxRedirect, ResultConsumer... resultConsumer) {

        Request checkedRequest = RequestBuilder.buildRequest(url, charSet, headers, trust, includeIndex, maxRedirect);

        return fetchPage(checkedRequest, resultConsumer);
    }

    public Result fetchPage(Request request, ResultConsumer... resultConsumers) {
        ResultConsumer resultConsumer = resultConsumers.length > 0 ? resultConsumers[0] : null;

        if (request.isCompleted()) {
            Result result = request.getResult();
            if (null != resultConsumer) {
                resultConsumer.accept(result);
            }
            return result;
        }

        String requestUrl = request.requestUrl();

        if (!request.isTrust()) {
            requestUrl = validUrl(request.requestUrl());
        }
        if (null != requestUrl) {
            requestUrl = URLCanonicalizer.getCanonicalURL(requestUrl);
        }
        if (null == requestUrl || requestUrl.trim().length() < 1) {
            request.setCompleted(new Result(request.getOrigUrl(), WebConst.LOCAL_NOT_ACCEPTABLE, "链接406: " + request.getOrigUrl()));
            if (null != resultConsumer) {
                resultConsumer.accept(request.getResult());
            }
            return request.getResult();
        }

        return getResult(requestUrl, request, resultConsumer);
    }

    private Result getResult(final String requestUrl, final Request request, ResultConsumer resultConsumer) {

        //判断是否网络断开
        ConnectionAbortUtils.checkNetworkStatus();

        long start = System.currentTimeMillis();
        if (null != resultConsumer && async) {
            AsyncHttpClient asyncHttpClient = (AsyncHttpClient) this;
            asyncHttpClient.async(requestUrl, request.getOrigUrl(), 0, request, result -> {
                if (isRetryAble(result, request)) {
                    asyncHttpClient.async(result.getUrl(), result.getUrl(), result.getRedirectCount(), request, resultConsumer);
                } else if (result.isRedirect() && result.getRedirectCount() < request.getMaxRedirect() && null != result.getMoveToUrl()) {
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

            int redirect = 0;
            Result result;

            String _requestUrl = requestUrl;

            while (true) {

                request.setHttpExecStartTime(System.currentTimeMillis());
                result = request(_requestUrl, request.getOrigUrl(), request);

                if (isRetryAble(result, request)) {
                    continue;
                }

                if (result.isRedirect() && redirect < request.getMaxRedirect() && null != result.getMoveToUrl()) {
                    _requestUrl = result.getMoveToUrl();
                    redirect++;
                } else {
                    break;
                }
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

    private boolean isRetryAble(Result result, Request request) {

        boolean networkDown = ConnectionAbortUtils.isNetworkOut(result, request);

        if (networkDown) {
            return true;
        }

        for (HttpRetryHandler retryHandler : HTTP_RETRY_HANDLER_SET) {
            if (retryHandler.isRetryAble(result)) {
                return true;
            }
        }
        return false;
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, boolean trust, ResultConsumer... resultConsumer) {
        return fetchPage(url, charSet, headers, trust, INDEX_DEFAULT_ACCESS, 0, resultConsumer);
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, ResultConsumer... resultConsumer) {
        return fetchPage(url, charSet, headers, INDEX_DEFAULT_ACCESS, 0, resultConsumer);
    }

    public Result fetchPage(String url, ResultConsumer... resultConsumer) {
        return fetchPage(url, 0, resultConsumer);
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, boolean trust, int maxRedirect, ResultConsumer... resultConsumer) {
        return fetchPage(url, charSet, headers, trust, INDEX_DEFAULT_ACCESS, maxRedirect, resultConsumer);
    }

    public Result fetchPage(String url, String charSet, Map<String, String> headers, int maxRedirect, ResultConsumer... resultConsumer) {
        return fetchPage(url, charSet, headers, INDEX_DEFAULT_ACCESS, maxRedirect, resultConsumer);
    }

    public Result fetchPage(String url, int maxRedirect, ResultConsumer... resultConsumer) {
        return fetchPage(url, null, null, false, maxRedirect, resultConsumer);
    }

    public void disableCookieSupport(String host) {
        if (StringUtils.isBlank(host)) {
            return;
        }
        cookieDisableHost.add(host);
    }

    public void enableCookieSupport(String host) {
        if (StringUtils.isBlank(host)) {
            return;
        }
        cookieDisableHost.remove(host);
    }


    public boolean isCookieRejected(String host) {
        return cookieDisableHost.contains(host);
    }


    public abstract Result request(String url, String oUrl, Request request);

    public abstract void registerCookie(String domain, String name, String value);

    protected boolean handException(Throwable e, String address, String url, String oUrl) {
        boolean isTimeOut = e instanceof SocketTimeoutException;
        _handException(e, url, oUrl);
        if (e instanceof UnknownHostException) {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            logger.error("读取页面异常URL:[{}] {} {} {}", address, url, oUrl, writer.toString());
            printWriter.close();
        } else {
            logger.error("读取页面异常URL:[{}] {} {} {}", address, url, oUrl, e);
        }
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
        return url.trim();
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
        result.charSet = entity.getFinalCharSet();
        result.bodyTruncatedWarning = entity.isBodyTruncatedWarning();
        if (result.bodyTruncatedWarning) {
            result.setMessage(entity.getWarningMsg());
            result.status = result.status + WebConst.HTTP_BODY_TRUNCATED_OFFSET;
        }
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