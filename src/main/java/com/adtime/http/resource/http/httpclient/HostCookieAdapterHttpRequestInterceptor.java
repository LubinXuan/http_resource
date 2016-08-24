package com.adtime.http.resource.http.httpclient;

import com.adtime.http.resource.WebConst;
import com.adtime.http.resource.WebResource;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Created by xuanlubin on 2016/8/24.
 */
public class HostCookieAdapterHttpRequestInterceptor extends RequestAddCookies {

    private WebResource resource;

    public HostCookieAdapterHttpRequestInterceptor(WebResource resource) {
        this.resource = resource;
    }

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        super.process(request, context);
        if (request instanceof HttpRequestWrapper) {
            HttpRequest _req = ((HttpRequestWrapper) request).getOriginal();
            if(_req instanceof HttpRequestBase){
                String host = ((HttpRequestBase) _req).getURI().getHost();
                if (resource.isCookieRejected(host)) {
                    request.removeHeaders(WebConst.COOKIE);
                }
            }
        }
    }
}
