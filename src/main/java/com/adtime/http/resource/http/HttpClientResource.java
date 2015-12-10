package com.adtime.http.resource.http;

import com.adtime.http.resource.*;
import com.adtime.http.resource.exception.DownloadStreamException;
import com.adtime.http.resource.url.URLCanonicalizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class HttpClientResource extends WebResource {

    private HttpClientHelper httpClientHelper;

    private Class<? extends HttpClientHelper> helperClass;

    public HttpClientResource(Class<? extends HttpClientHelper> helperClass) {
        this.helperClass = helperClass;
    }

    @Override
    protected boolean _handException(Throwable e, String url, String oUrl) {
        return e instanceof SocketException ||
                e instanceof ConnectTimeoutException ||
                e instanceof NoHttpResponseException ||
                e instanceof SocketTimeoutException ||
                e instanceof UnknownHostException ||
                e instanceof TruncatedChunkException;
    }


    @Override
    public void registerCookie(String domain, String name, String value) {
        httpClientHelper.registerCookie(domain, name, value);
    }

    @Override
    public Result request(String url, String oUrl, Request request) {
        return doRequest(url, oUrl, httpClientHelper.basic(), request);
    }

    private Result doRequest(String url, String oUrl, HttpClient client, Request request) {
        HttpRequestBase requestBase = null;
        HttpResponse response = null;
        try {
            if (Request.Method.GET.equals(request.getMethod())) {
                requestBase = new HttpGet(url);
            } else if (Request.Method.HEAD.equals(request.getMethod())) {
                requestBase = new HttpHead(url);
            } else {
                requestBase = new HttpPost(url);
                if (null != request.getRequestParam() && !request.getRequestParam().isEmpty()) {
                    List<NameValuePair> valuePairs = request.getRequestParam().entrySet().stream().map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
                    ((HttpPost) requestBase).setEntity(new UrlEncodedFormEntity(valuePairs));
                }
            }
            Map<String, String> _headers = request.getHeaderMap();
            _headers.forEach(requestBase::addHeader);

            requestBase.setConfig(httpClientHelper.requestConfig(request.getConnectionTimeout(), request.getReadTimeout()));

            response = client.execute(requestBase);

            Map<String, List<String>> headerMap = null;

            if (request.isReturnHeader() && null != response.getAllHeaders()) {
                headerMap = new HashMap<>();
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerMap.compute(header.getName(), (s, strings) -> {
                        if (null == strings) {
                            strings = new ArrayList<>();
                        }
                        HeaderElement[] headerElements = header.getElements();
                        for (HeaderElement headerElement : headerElements) {
                            if (!strings.contains(headerElement.getValue())) {
                                strings.add(headerElement.toString());
                            }
                        }
                        return strings;
                    });
                }
            }

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

    public void close(HttpResponse response, HttpRequestBase request) {
        if (null != response && null != response.getEntity()) {
            HttpEntity httpEntity = response.getEntity();
            try {
                InputStream inputStream = httpEntity.getContent();
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (null != request) {
            request.abort();
            request.releaseConnection();
        }
    }

    private Result handleSuccess(HttpResponse response, String charSet, String url, boolean checkBodySize) throws Exception {
        HttpEntity httpEntity = response.getEntity();
        ContentType contentType = ContentType.get(httpEntity);
        EntityReadUtils.Entity entity = EntityReadUtils.read(httpEntity, charSet, checkBodySize);
        Result tmp = new Result(url, entity.toString(url), false, response.getStatusLine().getStatusCode());
        if (null != contentType) {
            tmp.setContentType(contentType.getMimeType());
        }
        tmp.setCharSet(entity.getFinalCharSet());
        String refreshUrl = metaRefresh(tmp);
        if (StringUtils.isNotBlank(refreshUrl)) {
            Result result = new Result(url, "", true, HttpURLConnection.HTTP_MOVED_PERM);
            result.setMoveToUrl(URLCanonicalizer.mergePathUrl(url, refreshUrl));
            entity = null;
            return result;
        }
        copy(entity, tmp);
        entity = null;
        return tmp;
    }

    private Result handleRedirect(HttpResponse response, String url) throws Exception {
        Result result = new Result(url, "", true, response.getStatusLine().getStatusCode());
        Header header = response.getFirstHeader("Location");
        if (header != null) {
            String movedToUrl = URLCanonicalizer.mergePathUrl(url, header.getValue());
            result.setMoveToUrl(movedToUrl);
        }
        header = null;
        return result;
    }

    public void setHttpClientHelper(HttpClientHelper httpClientHelper) {
        this.httpClientHelper = httpClientHelper;
    }

    @Override
    public void setConfig(CrawlConfig config) {

        super.setConfig(config);

        if (null != httpClientHelper) {
            return;
        }
        try {
            this.httpClientHelper = helperClass.getConstructor(CrawlConfig.class).newInstance(config);
        } catch (Exception e) {
            throw new IllegalArgumentException("httpclient 初始化失败", e);
        }
    }
}
