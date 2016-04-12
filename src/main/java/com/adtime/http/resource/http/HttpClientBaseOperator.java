package com.adtime.http.resource.http;

import com.adtime.http.resource.*;
import com.adtime.http.resource.url.URLCanonicalizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public abstract class HttpClientBaseOperator extends WebResource {

    @Override
    protected boolean _handException(Throwable e, String url, String oUrl) {
        return e instanceof SocketException ||
                e instanceof ConnectTimeoutException ||
                e instanceof NoHttpResponseException ||
                e instanceof SocketTimeoutException ||
                e instanceof UnknownHostException ||
                e instanceof TruncatedChunkException;
    }

    protected HttpRequestBase create(String url, Request request) {
        url = Request.toHttpclientSafeUrl(url);
        HttpRequestBase requestBase;
        if (Request.Method.GET.equals(request.getMethod())) {
            requestBase = new HttpGet(url);
        } else if (Request.Method.HEAD.equals(request.getMethod())) {
            requestBase = new HttpHead(url);
        } else {
            requestBase = new HttpPost(url);
            if (null != request.getRequestParam() && !request.getRequestParam().isEmpty()) {
                List<NameValuePair> valuePairs = request.getRequestParam().entrySet().stream().map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
                try {
                    ((HttpPost) requestBase).setEntity(new UrlEncodedFormEntity(valuePairs));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        Map<String, String> _headers = request.getHeaderMap();
        for (Map.Entry<String, String> entry : _headers.entrySet()) {
            if (WebConst.COOKIE.equals(entry.getKey())) {
                requestBase.setHeader(entry.getKey(), entry.getValue());
            } else {
                requestBase.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return requestBase;
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

    protected Result handleSuccess(HttpResponse response, String charSet, String url, boolean checkBodySize) throws Exception {
        HttpEntity httpEntity = response.getEntity();
        ContentType contentType = null;
        try {
            contentType = ContentType.get(httpEntity);
        } catch (Throwable ignore) {

        }
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

    protected Result handleRedirect(HttpResponse response, String url) throws Exception {
        Result result = new Result(url, "", true, response.getStatusLine().getStatusCode());
        Header header = response.getFirstHeader("Location");
        if (header != null) {
            String movedToUrl = URLCanonicalizer.mergePathUrl(url, header.getValue());
            result.setMoveToUrl(movedToUrl);
        }
        header = null;
        return result;
    }

    protected Map<String, List<String>> readHeader(Request request, HttpResponse response) {
        if (request.isReturnHeader() && null != response.getAllHeaders()) {
            Map<String, List<String>> headerMap = new HashMap<>();
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
            return headerMap;
        } else {
            return Collections.emptyMap();
        }
    }

}