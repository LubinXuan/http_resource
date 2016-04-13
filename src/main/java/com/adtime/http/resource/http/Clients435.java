package com.adtime.http.resource.http;

import com.adtime.http.resource.extend.DynamicProxyHttpRoutePlanner;
import com.adtime.http.resource.proxy.DynamicProxyProvider;
import com.adtime.http.resource.util.SSLSocketUtil;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.net.ssl.SSLException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Apache HttpClient 4.3.5
 */
public class Clients435 extends HttpClientHelper {

    private static Logger logger = LoggerFactory.getLogger(Clients435.class);

    private static List<Header> DEFAULT_HEADERS = new ArrayList<>();

    static {
        DEFAULT_HEADERS.add(new BasicHeader(HttpHeaders.CONNECTION, "close"));
        //不使用GZIP。。。部分网站响应GZIP 会导致不返回...
        //defaultHeaders.add(new BasicHeader(HttpHeaders.ACCEPT_ENCODING, "gzip"));
    }

    private RequestConfig.Builder requestConfigBuilder;
    private CredentialsProvider credentialsProvider = null;
    private RequestConfig defaultRequestConfig = null;
    private CookieStore cookieStore = new BasicCookieStore();

    private PoolingHttpClientConnectionManager connectionManager = null;
    private PoolingNHttpClientConnectionManager nHttpClientConnectionManager = null;


    private ConnectionConfig connectionConfig = null;
    private HttpRequestRetryHandler retryHandler = null;


    private HttpRoutePlanner routePlanner;

    @Autowired(required = false)
    private DynamicProxyProvider dynamicProxyProvider;

    @Override
    public void registerCookie(String domain, String name, String value) {
        cookieStore.addCookie(newClientCookie(domain, name, value));
    }

    private boolean init = false;

    public void init() {

        if (init) {
            return;
        }

        logger.info("HttpClient Version 4.3.5");

        if (null != dynamicProxyProvider) {
            this.routePlanner = new DynamicProxyHttpRoutePlanner(new DefaultSchemePortResolver(), dynamicProxyProvider);
        }

        requestConfigBuilder = RequestConfig.custom()
                .setSocketTimeout(config.getSocketTimeout())
                .setConnectTimeout(config.getConnectionTimeout())
                .setConnectionRequestTimeout(config.getConnectionTimeout())
                .setRedirectsEnabled(config.isFollowRedirects())
                .setCircularRedirectsAllowed(false)
                .setExpectContinueEnabled(false)
                .setCookieSpec(CookieSpecs.DEFAULT);

        MessageConstraints messageConstraints = MessageConstraints.custom()
                .setMaxHeaderCount(200)
                .build();

        connectionConfig = ConnectionConfig.custom()
                .setMalformedInputAction(CodingErrorAction.IGNORE)
                .setUnmappableInputAction(CodingErrorAction.IGNORE)
                .setCharset(Consts.UTF_8)
                .setMessageConstraints(messageConstraints)
                .build();


        if (config.getProxyHost() != null) {

            if (null != config.getProxyUsername()) {
                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(config.getProxyHost(), config.getProxyPort()),
                        new UsernamePasswordCredentials(config.getProxyUsername(), config.getProxyPassword()));
            }
        }

        retryHandler = (exception, executionCount, context) -> {
            if (executionCount >= 3) {
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // 超时
                return false;
            }
            if (exception instanceof UnknownHostException) {
                // 目标服务器不可达
                return false;
            }
            if (exception instanceof SSLException) {
                // ssl握手异常
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            return !(request instanceof HttpEntityEnclosingRequest);
        };
        defaultRequestConfig = requestConfigBuilder.build();

        init = true;
    }

    @Override
    public HttpClientBuilder createHttpClientBuilder() {
        if (null == connectionManager) {
            RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
            registryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);
            if (config.isIncludeHttpsPages()) {
                try {
                    registryBuilder.register("https", new SSLConnectionSocketFactory(SSLSocketUtil.getSSLContext(), SSLSocketUtil.defaultHostnameVerifier()));
                } catch (Exception e) {
                    logger.error("Https Registry Fail : {}", e.toString());
                }
            }
            connectionManager = new PoolingHttpClientConnectionManager(registryBuilder.build(), randomDns());
            connectionManager.setDefaultConnectionConfig(connectionConfig);
            connectionManager.setMaxTotal(config.getMaxTotalConnections());
            connectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());
            new IdleConnectionMonitor(connectionManager).start();
        }
        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig)
                .setUserAgent(config.getUserAgentString())
                .setConnectionManager(connectionManager)
                .setRetryHandler(retryHandler).setDefaultHeaders(DEFAULT_HEADERS)
                .setDefaultCookieStore(cookieStore).disableContentCompression();

        if (null != credentialsProvider) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }
        if (null != routePlanner) {
            builder.setRoutePlanner(routePlanner);
        }

        return builder;
    }

    @Override
    public HttpAsyncClientBuilder createHttpAsyncClientBuilder() throws IOReactorException {
        if (null == nHttpClientConnectionManager) {
            ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
            RegistryBuilder<SchemeIOSessionStrategy> registryBuilder = RegistryBuilder.create();
            registryBuilder.register("http", NoopIOSessionStrategy.INSTANCE);
            if (config.isIncludeHttpsPages()) {
                try {
                    registryBuilder.register("https", new SSLIOSessionStrategy(SSLSocketUtil.getSSLContext(), SSLSocketUtil.defaultHostnameVerifier()));
                } catch (Exception e) {
                    logger.error("Https Registry Fail : {}", e.toString());
                }
            }
            nHttpClientConnectionManager = new PoolingNHttpClientConnectionManager(ioReactor);
            nHttpClientConnectionManager.setMaxTotal(config.getMaxTotalConnections());
            nHttpClientConnectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());
            nHttpClientConnectionManager.setDefaultConnectionConfig(connectionConfig);
        }
        HttpAsyncClientBuilder asyncClientBuilder = HttpAsyncClients.custom();
        asyncClientBuilder.setDefaultRequestConfig(defaultRequestConfig)
                .setUserAgent(config.getUserAgentString())
                .setDefaultCookieStore(cookieStore);

        asyncClientBuilder.setConnectionManager(nHttpClientConnectionManager);

        if (null != credentialsProvider) {
            asyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
        if (null != routePlanner) {
            asyncClientBuilder.setRoutePlanner(routePlanner);
        }

        return asyncClientBuilder;
    }

    private static final Map<String, RequestConfig> REQUEST_CONFIG_MAP = new ConcurrentHashMap<>();

    @Override
    public RequestConfig requestConfig(Integer connectTimeout, Integer readTimeout) {
        if (null == connectTimeout && null == readTimeout) {
            return null;
        }

        String key = connectTimeout + "&" + readTimeout;

        return REQUEST_CONFIG_MAP.compute(key, (s, requestConfig) -> {

            if (null != requestConfig) {
                return requestConfig;
            }

            if (null != connectTimeout) {
                requestConfigBuilder.setConnectTimeout(connectTimeout);
            }

            if (null != readTimeout) {
                requestConfigBuilder.setSocketTimeout(readTimeout);
            }


            return requestConfigBuilder.build();
        });
    }


    private static class IdleConnectionMonitor extends Thread {

        private boolean shutDown = false;

        private HttpClientConnectionManager connMgr;

        IdleConnectionMonitor(HttpClientConnectionManager cmr) {
            this.connMgr = cmr;
        }

        @Override
        public void run() {
            try {
                while (!shutDown) {
                    synchronized (this) {
                        wait(5000);
                        closeConnections();
                    }
                }
                connMgr.shutdown();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        private void closeConnections() {
            connMgr.closeExpiredConnections();
            connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
        }

        public void shutdown() {
            this.shutDown = false;
        }
    }
}
