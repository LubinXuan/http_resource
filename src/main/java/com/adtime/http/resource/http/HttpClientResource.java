package com.adtime.http.resource.http;

import com.adtime.http.resource.Request;
import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebConst;
import com.adtime.http.resource.exception.DownloadStreamException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

import java.net.HttpURLConnection;
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
        httpClient = httpClientHelper.createHttpClientBuilder().build();
    }

    @Override
    public void registerCookie(String domain, String name, String value) {
        httpClientHelper.registerCookie(domain, name, value);
    }

    @Override
    public Result request(String url, String oUrl, Request request) {
        return doRequest(url, oUrl, httpClient, request);
    }

    private Result doRequest(String url, String oUrl, HttpClient client, Request request) {
        url = Request.toHttpclientSafeUrl(url);
        HttpRequestBase requestBase = create(url, request);
        HttpResponse response = null;
        try {
            requestBase.setConfig(httpClientHelper.requestConfig(request.getConnectionTimeout(), request.getReadTimeout()));

            response = client.execute(requestBase);

            Map<String, List<String>> headerMap = readHeader(request, response);

            int sts = response.getStatusLine().getStatusCode();

            if (sts == HttpURLConnection.HTTP_MOVED_PERM || sts == HttpURLConnection.HTTP_MOVED_TEMP) {
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
            handException(e, url, oUrl);
            if (e instanceof DownloadStreamException) {
                return new Result(url, WebConst.DOWNLOAD_STREAM, e.toString());
            }
            return new Result(url, WebConst.HTTP_ERROR, e.toString());
        } finally {
            close(response, requestBase);
        }
    }

    public void setHttpClientHelper(HttpClientHelper httpClientHelper) {
        this.httpClientHelper = httpClientHelper;
    }

}
