package com.adtime.http.resource;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lubin.Xuan on 2015/7/13.
 * ie.
 */
public class Request implements Serializable {
    public enum Method {
        POST, GET, HEAD
    }

    private String url;
    private String origUrl;
    private String requestUrl;
    private Method method;
    private Map<String, String> headerMap = new HashMap<>();
    private Map<String, String> requestParam = new HashMap<>();
    private String charSet;
    private int maxRedirect;

    private boolean returnHeader;

    private boolean trust;

    private boolean requestAsMobile = false;

    private boolean completed = false;
    private Result result = null;
    private boolean allowIndex;


    private boolean checkBodySize;

    private Integer connectionTimeout;
    private Integer readTimeout;

    public Request() {
    }

    public Request(boolean requestAsMobile) {
        this.requestAsMobile = requestAsMobile;
    }

    public String getOrigUrl() {
        return origUrl;
    }

    public void setOrigUrl(String origUrl) {
        this.origUrl = origUrl;
    }

    public String getCharSet() {
        return charSet;
    }

    public void setCharSet(String charSet) {
        this.charSet = charSet;
    }

    public int getMaxRedirect() {
        return maxRedirect;
    }

    public void setMaxRedirect(int maxRedirect) {
        if (Method.HEAD.equals(this.method)) {
            this.maxRedirect = 0;
        } else {
            this.maxRedirect = maxRedirect;
        }
    }

    protected String buildGetParameterUrl() {
        StringBuilder stringBuilder = new StringBuilder(RequestUtil.buildGetParameter(this.getRequestParam()));
        if (stringBuilder.length() > 0) {
            if (!url.contains("?")) {
                stringBuilder.insert(0, "?");
            } else if (!url.endsWith("&")) {
                stringBuilder.insert(0, "&");
            }
        }
        stringBuilder.insert(0, url);
        return stringBuilder.toString();
    }

    public String requestUrl() {
        if (Method.POST.equals(method)) {
            return url;
        }
        if (requestParam.isEmpty()) {
            return url;
        } else {
            if (null == requestUrl) {
                synchronized (this) {
                    requestUrl = buildGetParameterUrl();
                }
            }
            return requestUrl;
        }
    }

    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
        if (Method.HEAD.equals(this.method)) {
            this.maxRedirect = 0;
            this.returnHeader = true;
        }
    }

    public Map<String, String> getHeaderMap() {
        return null == headerMap ? Collections.emptyMap() : headerMap;
    }

    public void setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    public Map<String, String> getRequestParam() {
        return requestParam;
    }

    public void setRequestParam(Map<String, String> requestParam) {
        this.requestParam = requestParam;
    }

    public void setUrl(String url) {
        if (null != url && !url.startsWith("http://") && !url.startsWith("https://")) {
            this.url = "http://" + url;
        } else {
            this.url = url;
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(Result result) {
        this.completed = true;
        this.result = result;
    }

    public boolean isReturnHeader() {
        return returnHeader;
    }

    public void setReturnHeader(boolean returnHeader) {
        this.returnHeader = returnHeader;
    }

    protected void setTrust(boolean trust) {
        this.trust = trust;
    }

    public boolean isTrust() {
        return trust;
    }

    public boolean isRequestAsMobile() {
        return requestAsMobile;
    }

    public boolean isCheckBodySize() {
        return checkBodySize;
    }

    public void setCheckBodySize(boolean checkBodySize) {
        this.checkBodySize = checkBodySize;
    }

    public Result getResult() {
        return result;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setHeader(String headerName, String headerValue) {
        this.headerMap.put(headerName, headerValue);
    }
}
