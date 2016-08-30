package com.adtime.http.resource.url.format;

import com.adtime.http.resource.Request;

/**
 * Created by xuanlubin on 2016/8/30.
 */
public interface UrlFormat<T> {

    String format(String url, T seed);

    boolean updateRequestParam(Request request, T t);
}
