package com.adtime.http.resource.http;

import com.adtime.http.resource.*;
import com.adtime.http.resource.exception.DownloadStreamException;
import com.adtime.http.resource.extend.DynamicProxySelector;
import com.adtime.http.resource.url.URLCanonicalizer;
import com.adtime.http.resource.url.URLInetAddress;
import com.adtime.http.resource.util.HttpUtil;
import com.adtime.http.resource.util.SSLSocketUtil;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Map;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class HttpUrlConnectionResource extends WebResource {

    private final static SSLSocketFactory sslSocketFactory;

    static {
        sslSocketFactory = SSLSocketUtil.getSSLContext().getSocketFactory();
        System.setProperty("http.maxConnections", "5");
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    private static final CookieStore cookieStore = new MemoryCookieStore();

    @Override
    protected boolean _handException(Throwable e, String url, String oUrl) {
        return e instanceof SocketException ||
                e instanceof SocketTimeoutException ||
                e instanceof UnknownHostException;
    }

    @Override
    public void registerCookie(String domain, String name, String value) {
        HttpCookie cookie = new HttpCookie(name, value);
        cookie.setDomain(domain);
        cookie.setPath("/");
        cookie.setVersion(0);
        cookieStore.add(null, cookie);
    }

    @Override
    public void clearAllCookie() {
        cookieStore.removeAll();
    }

    @PostConstruct
    public void _init() {
        ProxySelector.setDefault(new DynamicProxySelector(dynamicProxyProvider));
        if (config.getProxyHost() != null) {
            if (null != config.getProxyUsername()) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(config.getProxyUsername(), config.getProxyPassword().toCharArray());
                    }
                });
            }
        }
    }

    @Override
    public Result request(String url, String oUrl, Request request) {
        return doRequest(url, url, config, request);
    }

    public Result doRequest(String targetUrl, String oUrl, CrawlConfig config, Request request) {
        int retryCount = config.getRetryCount();

        try {
            URL url = URLInetAddress.create(targetUrl);
            return __send(request, url, targetUrl, oUrl, retryCount);
        } catch (Exception e) {
            handException(e, null, targetUrl, oUrl);
            return new Result(targetUrl, WebConst.HTTP_ERROR, e.toString());
        }

    }

    private Result __send(Request request, URL url, String targetUrl, String oUrl, int retryCount) {
        Result result;
        do {
            boolean isTimeOut = false;
            HttpURLConnection con = null;
            try {
                con = configConnectionAndSend(request, url);

                int sts = con.getResponseCode();

                Map<String, List<String>> headerMap = con.getHeaderFields();

                saveCookie(con, url);

                if (HttpUtil.isRedirect(sts)) {
                    return handleRedirect(con, targetUrl).withHeader(headerMap);
                } else {
                    if (Request.Method.HEAD.equals(request.getMethod())) {
                        return new Result(targetUrl, sts, "").withHeader(headerMap);
                    } else {
                        if (sts == HttpURLConnection.HTTP_OK) {
                            return handleSuccess(con, request.getCharSet(), targetUrl, request.isCheckBodySize()).withHeader(headerMap);
                        } else if (sts >= 400) {
                            return handleError(con, request.getCharSet(), targetUrl).withHeader(headerMap);
                        } else {
                            return new Result(targetUrl, "", false, sts).withHeader(headerMap);
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof DownloadStreamException) {
                    return new Result(targetUrl, WebConst.DOWNLOAD_STREAM, e.toString());
                }
                isTimeOut = handException(e, url.getAuthority(), targetUrl, oUrl);
                result = new Result(targetUrl, WebConst.HTTP_ERROR, e.toString());
            } finally {
                closeHttpURLConnection(con);
            }
            if (!isTimeOut) {
                break;
            }
            retryCount--;
        } while (retryCount > 0);
        return result;
    }

    private HttpURLConnection configConnectionAndSend(Request request, URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url.toExternalForm()).openConnection();
        con.setRequestMethod(request.getMethod().name());
        con.setDoInput(true);
        con.setUseCaches(false);
        con.setInstanceFollowRedirects(false);
        if (null != request.getConnectionTimeout()) {
            con.setConnectTimeout(request.getConnectionTimeout());
        } else {
            con.setConnectTimeout(config.getConnectionTimeout());
        }
        if (null != request.getReadTimeout()) {
            con.setReadTimeout(request.getReadTimeout());
        } else {
            con.setReadTimeout(config.getSocketTimeout());
        }
        Map<String, String> default_headers = request.getHeaderMap();
        for (Map.Entry<String, String> entry : default_headers.entrySet()) {
            if (WebConst.COOKIE.equals(entry.getKey())) {
                con.setRequestProperty(entry.getKey(), entry.getValue());
            } else {
                con.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        con.setRequestProperty("Host", url.getHost());

        if (con instanceof HttpsURLConnection) {
            if (null == sslSocketFactory) {
                throw new SSLException("SSLSocketFactory 没有被初始化!!!");
            }
            ((HttpsURLConnection) con).setSSLSocketFactory(sslSocketFactory);
        }

        setCookie(con, url);

        if (Request.Method.POST.equals(request.getMethod()) && null != request.getRequestParam() && !request.getRequestParam().isEmpty()) {
            con.setDoOutput(true);
            con.getOutputStream().write(RequestUtil.buildGetParameter(request.getRequestParam()).getBytes("utf-8"));
        }
        return con;
    }


    private void closeHttpURLConnection(HttpURLConnection con) {
        if (null != con) {
            try {
                if (null != con.getInputStream()) {
                    con.getInputStream().close();
                }
            } catch (Exception ignore) {
            }

            try {
                if (null != con.getErrorStream()) {
                    con.getErrorStream().close();
                }
            } catch (Exception ignore) {
            }

            try {
                if (null != con.getOutputStream()) {
                    con.getOutputStream().close();
                }
            } catch (Exception ignore) {
            }

            con.disconnect();
        }
    }


    private Result handleError(HttpURLConnection con, String charSet, String url) throws Exception {
        return getResult(con, true, charSet, url, true);
    }

    private Result getResult(HttpURLConnection con, boolean error, String charSet, String url, boolean checkBodySize) throws Exception {
        String header_contentType = con.getContentType();
        if (null != header_contentType) {
            header_contentType = header_contentType.replaceAll("\\s", "");
        }

        String contentType = null;
        String contentCharset = null;
        if (null != header_contentType) {
            String[] part = StringUtils.splitByWholeSeparator(header_contentType.toLowerCase(), ";charset=");
            if (part.length == 2) {
                contentType = part[0];
                contentCharset = part[1];
            } else {
                contentType = header_contentType;
            }
        }

        if (null == charSet && null != contentCharset) {
            charSet = contentCharset;
        }

        EntityReadUtils.Entity entity = EntityReadUtils.read(con, error, charSet, checkBodySize);
        String content = entity.toString(url);
        Result tmp = new Result(url, content, false, con.getResponseCode())
                .setContentType(contentType);
        copy(entity, tmp);
        entity = null;
        return tmp;
    }

    private Result handleSuccess(HttpURLConnection con, String charSet, String url, boolean checkBodySize) throws Exception {
        Result tmp = getResult(con, false, charSet, url, checkBodySize);
        String refreshUrl = metaRefresh(tmp);
        if (refreshUrl.trim().length() > 0) {
            Result result = new Result(url, "", true, HttpURLConnection.HTTP_MOVED_PERM);
            result.setMoveToUrl(URLCanonicalizer.mergePathUrl(url, refreshUrl));
            return result;
        }
        return tmp;
    }

    private Result handleRedirect(HttpURLConnection con, String url) throws Exception {
        Result result = new Result(url, "", true, con.getResponseCode());
        String reLocation = con.getHeaderField("Location");
        if (reLocation != null) {
            String movedToUrl = URLCanonicalizer.resolveRedirect(url, reLocation);
            result.setMoveToUrl(movedToUrl);
        }
        return result;
    }

    private void setCookie(HttpURLConnection connection, URL url) {

        if (cookieDisableHost.contains(url.getHost())) {
            return;
        }

        try {
            URI uri = new URI(url.getProtocol(), url.getHost(), null, null, null);
            List<HttpCookie> cookies = cookieStore.get(uri);
            if (null != cookies && !cookies.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (HttpCookie cookie : cookies) {

                    if (null != cookie.getDomain() && cookieDisableHost.contains(cookie.getDomain())) {
                        continue;
                    }

                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append(cookie.getName()).append("=").append(cookie.getValue());
                }

                if (sb.length() == 0) {
                    return;
                }

                String cookie = connection.getRequestProperty("Cookie");
                if (null != cookie) {
                    cookie = cookie + "; " + sb.toString();
                } else {
                    cookie = sb.toString();
                }
                connection.addRequestProperty("Cookie", cookie);
                logger.debug("host:{} path:{} cookie:{}", url.getHost(), url.getPath(), cookie);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void saveCookie(HttpURLConnection connection, URL url) {
        try {
            URI uri = new URI(url.getProtocol(), url.getHost(), null, null, null);
            List<String> newCookies = connection.getHeaderFields().get("Set-Cookie");
            if (null != newCookies) {
                for (String ck : newCookies) {
                    List<HttpCookie> cookies = HttpCookie.parse(ck);
                    for (HttpCookie cookie : cookies) {
                        cookieStore.add(uri, cookie);
                    }
                }
            }
        } catch (Throwable ignore) {

        }

    }

}
