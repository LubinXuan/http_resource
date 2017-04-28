package com.adtime.http.resource.http.nio;

import com.adtime.http.resource.Request;

import java.net.URL;

/**
 * Created by xuanlubin on 2017/4/27.
 */
public class RequestWrap {
    private Request request;
    private URL url;
    private ResponseCallback responseCallback;


    public RequestWrap(Request request, URL url, ResponseCallback responseCallback) {
        this.request = request;
        this.url = url;
        this.responseCallback = responseCallback;
    }

    public URL getUrl() {
        return url;
    }

    public Request getRequest() {
        return request;
    }

    public ResponseCallback getResponseCallback() {
        return responseCallback;
    }
}
