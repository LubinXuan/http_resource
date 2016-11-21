package com.adtime.http.resource.connection;

import com.adtime.http.resource.Result;

/**
 * Created by xuanlubin on 2016/11/21.
 */
public interface HttpRetryHandler {
    boolean isRetryAble(Result result);
}
