package com.adtime.http.resource;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
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
    private Result result;
    private boolean allowIndex;


    private boolean checkBodySize;

    private Integer connectionTimeout;
    private Integer readTimeout;

    private long httpExecStartTime;

    public Request() {
    }

    public Request(boolean requestAsMobile) {
        this.requestAsMobile = requestAsMobile;
    }

    public String getOrigUrl() {
        return origUrl;
    }

    public Request setOrigUrl(String origUrl) {
        this.origUrl = origUrl;
        return this;
    }

    public String getCharSet() {
        return charSet;
    }

    public Request setCharSet(String charSet) {
        this.charSet = charSet;
        return this;
    }

    public int getMaxRedirect() {
        return maxRedirect;
    }

    public Request setMaxRedirect(int maxRedirect) {
        if (Method.HEAD.equals(this.method)) {
            this.maxRedirect = 0;
        } else {
            this.maxRedirect = maxRedirect;
        }
        return this;
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

    public Request setMethod(Method method) {
        this.method = method;
        if (Method.HEAD.equals(this.method)) {
            this.maxRedirect = 0;
            this.returnHeader = true;
        }
        return this;
    }

    public Map<String, String> getHeaderMap() {
        return null == headerMap ? Collections.emptyMap() : headerMap;
    }

    public Request setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
        return this;
    }

    public Map<String, String> getRequestParam() {
        return requestParam;
    }

    public Request setRequestParam(Map<String, String> requestParam) {
        this.requestParam = requestParam;
        return this;
    }

    public long getHttpExecStartTime() {
        return httpExecStartTime;
    }

    public void setHttpExecStartTime(long httpExecStartTime) {
        this.httpExecStartTime = httpExecStartTime;
    }

    public Request setUrl(String url) {
        if (null != url && !url.startsWith("http://") && !url.startsWith("https://")) {
            this.url = "http://" + url;
        } else {
            this.url = url;
        }
        if (StringUtils.isBlank(this.origUrl)) {
            this.origUrl = this.url;
        }
        return this;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Request setCompleted(Result result) {
        this.completed = true;
        this.result = result;
        return this;
    }

    public boolean isReturnHeader() {
        return returnHeader;
    }

    public Request setReturnHeader(boolean returnHeader) {
        this.returnHeader = returnHeader;
        return this;
    }

    public Request setTrust(boolean trust) {
        this.trust = trust;
        return this;
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

    public Request setCheckBodySize(boolean checkBodySize) {
        this.checkBodySize = checkBodySize;
        return this;
    }

    public Result getResult() {
        return result;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public Request setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public Request setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public Request setHeader(String headerName, String headerValue) {
        this.headerMap.put(headerName, String.valueOf(headerValue));
        return this;
    }

    public Request addParam(String paramName, Object paramValue) {
        this.requestParam.put(paramName, String.valueOf(paramValue));
        return this;
    }
}
