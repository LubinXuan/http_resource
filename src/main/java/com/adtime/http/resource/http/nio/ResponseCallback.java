package com.adtime.http.resource.http.nio;

import com.adtime.http.resource.Request;
import com.adtime.http.resource.Result;

/**
 * Created by xuanlubin on 2017/4/27.
 */
public interface ResponseCallback {
    void success(Request request, Result result);

    void failure(Request request, Result result,Throwable throwable);
}
