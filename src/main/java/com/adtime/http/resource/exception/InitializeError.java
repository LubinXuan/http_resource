package com.adtime.http.resource.exception;

/**
 * Created by Administrator on 2015/12/10.
 */
public class InitializeError extends Error {
    public InitializeError(String s, Throwable throwable) {
        super(s, throwable);
    }
}
