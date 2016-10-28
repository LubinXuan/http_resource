package com.adtime.http.resource.http;

import com.adtime.http.resource.Request;
import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebConst;
import com.adtime.http.resource.exception.DownloadStreamException;
import com.adtime.http.resource.http.httpclient.HostCookieAdapterHttpRequestInterceptor;
import com.adtime.http.resource.util.HttpUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class HttpClientResource extends HttpClientBaseOperator {


    private HttpClient httpClient;

    public HttpClientResource(HttpClientHelper httpClientHelper) {
        super(httpClientHelper);
        httpClient = httpClientHelper.createHttpClientBuilder(this).build();
    }

    @Override
    public Result request(String url, String oUrl, Request request) {
        return doRequest(url, oUrl, httpClient, request);
    }

    private Result doRequest(String url, String oUrl, HttpClient client, Request request) {
        RequestWrap requestBase;
        try {
            requestBase = create(url, request);
        } catch (MalformedURLException | UnknownHostException | URISyntaxException e) {
            handException(e, null, url, oUrl);
            return new Result(url, WebConst.HTTP_ERROR, e.toString());
        }

        HttpResponse response = null;
        try {
            requestBase.request.setConfig(httpClientHelper.requestConfig(request.getConnectionTimeout(), request.getReadTimeout()));
            response = client.execute(requestBase.target, requestBase.request,requestBase.context);
            Map<String, List<String>> headerMap = readHeader(request, response);
            int sts = response.getStatusLine().getStatusCode();
            if (HttpUtil.isRedirect(sts)) {
                return handleRedirect(response, url);
            } else {
                if (Request.Method.HEAD.equals(request.getMethod())) {
                    return new Result(url, sts, "").withHeader(headerMap);
                } else {
                    if (sts == HttpURLConnection.HTTP_OK) {
                        return handleSuccess(response, request.getCharSet(), url, request.isCheckBodySize()).withHeader(headerMap);
                    } else if (sts >= 400) {
                        return handleSuccess(response, request.getCharSet(), url, request.isCheckBodySize()).withHeader(headerMap);
                    } else {
                        return new Result(url, "", false, sts).withHeader(headerMap);
                    }
                }
            }
        } catch (Throwable e) {
            handException(e, requestBase.request.getURI().getAuthority(), url, oUrl);
            if (e instanceof DownloadStreamException) {
                return new Result(url, WebConst.DOWNLOAD_STREAM, e.toString());
            }
            return new Result(url, WebConst.HTTP_ERROR, e.toString());
        } finally {
            close(response, requestBase.request);
        }
    }

    public void setHttpClientHelper(HttpClientHelper httpClientHelper) {
        this.httpClientHelper = httpClientHelper;
    }

}
