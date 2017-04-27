package com.adtime.http.resource.http.nio;

import com.adtime.http.resource.Request;

/**
 * Created by xuanlubin on 2017/4/27.
 */
public class RequestWrap {
    private Request request;
    private ResponseCallback responseCallback;


    public RequestWrap(Request request, ResponseCallback responseCallback) {
        this.request = request;
        this.responseCallback = responseCallback;
    }

    public Request getRequest() {
        return request;
    }

    public ResponseCallback getResponseCallback() {
        return responseCallback;
    }
}
