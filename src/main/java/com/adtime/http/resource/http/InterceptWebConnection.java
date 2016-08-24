package com.adtime.http.resource.http;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.FalsifyingWebConnection;

import java.io.IOException;

/**
 * Created by xuanlubin on 2016/8/22.
 */
public class InterceptWebConnection extends FalsifyingWebConnection {
    public InterceptWebConnection(WebClient webClient) throws IllegalArgumentException {
        super(webClient);
    }

    @Override
    public WebResponse getResponse(WebRequest request) throws IOException {
        WebResponse response = super.getResponse(request);
        if (response.getWebRequest().getUrl().toString().contains("google")) {
            return createWebResponse(response.getWebRequest(), "", "application/javascript", 200, "Ok");
        }
        return super.getResponse(request);
    }
}