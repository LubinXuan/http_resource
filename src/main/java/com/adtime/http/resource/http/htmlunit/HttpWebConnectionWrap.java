package com.adtime.http.resource.http.htmlunit;

import com.adtime.http.resource.http.HttpClientHelper;
import com.gargoylesoftware.htmlunit.HttpWebConnection;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by xuanlubin on 2016/10/20.
 */
public class HttpWebConnectionWrap implements WebConnection {

    private static final Method getHttpClientBuilder;

    static {
        try {
            getHttpClientBuilder = HttpWebConnection.class.getDeclaredMethod("getHttpClientBuilder");
            getHttpClientBuilder.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final HttpWebConnection webConnection;

    public HttpWebConnectionWrap(HttpWebConnection webConnection, HttpRoutePlanner routePlanner) {
        this.webConnection = webConnection;
        try {
            HttpClientBuilder builder = (HttpClientBuilder) getHttpClientBuilder.invoke(this.webConnection);
            builder.setDnsResolver(HttpClientHelper.randomDns());
            builder.setRoutePlanner(routePlanner);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WebResponse getResponse(WebRequest webRequest) throws IOException {
        return webConnection.getResponse(webRequest);
    }

    @Override
    public void close() throws Exception {
        webConnection.close();
    }
}
