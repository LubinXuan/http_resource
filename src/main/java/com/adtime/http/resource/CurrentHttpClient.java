package com.adtime.http.resource;

import org.apache.http.client.HttpClient;

public class CurrentHttpClient {
    private static final ThreadLocal<HttpClient> CURRENT_HTTPCLIENT = new ThreadLocal<>();

    public static HttpClient getCurrentHttpclient() {
        return CURRENT_HTTPCLIENT.get();
    }

    public static void setCurrentHttpclient(HttpClient httpClient) {
        CURRENT_HTTPCLIENT.set(httpClient);
    }
}
