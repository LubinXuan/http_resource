package com.adtime.http.resource.http;

import com.adtime.http.resource.CrawlConfig;
import com.adtime.http.resource.util.SSLSocketUtil;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Apache HttpClient 4.3.5
 */
public class Clients435 extends HttpClientHelper {

    private static Logger logger = LoggerFactory.getLogger(Clients435.class);

    private HttpClient httpClient;

    private static final Object lock = new Object();

    private RequestConfig.Builder requestConfigBuilder;
    private RequestConfig defaultRequestConfig;
    private PoolingHttpClientConnectionManager connectionManager;
    private ArrayList<Header> defaultHeaders;
    private HttpRequestRetryHandler retryHandler;
    private CredentialsProvider credentialsProvider = null;
    private CookieStore cookieStore = new BasicCookieStore();

    private boolean init = false;

    public Clients435(CrawlConfig config) {
        super(config);
    }

    @Override
    public HttpClient basic() {
        if (null == httpClient) {
            synchronized (this) {
                if (null == httpClient) {
                    httpClient = _init();
                }
            }
        }
        return httpClient;
    }

    @Override
    public HttpClient newBasic() {
        init();
        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig)
                .setUserAgent(config.getUserAgentString())
                .setConnectionManager(connectionManager)
                .setRetryHandler(retryHandler).setDefaultHeaders(defaultHeaders)
                .setDefaultCookieStore(cookieStore).disableContentCompression();
        if (null != credentialsProvider) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }
        return builder.build();
    }

    @Override
    public void registerCookie(String domain, String name, String value) {
        cookieStore.addCookie(newClientCookie(domain, name, value));
    }

    @PostConstruct
    public synchronized void init() {

        if (init) {
            return;
        }

        logger.info("HttpClient Version 4.3.5");

        requestConfigBuilder = RequestConfig.custom()
                .setSocketTimeout(config.getSocketTimeout())
                .setConnectTimeout(config.getConnectionTimeout())
                .setConnectionRequestTimeout(config.getConnectionTimeout())
                .setRedirectsEnabled(config.isFollowRedirects())
                .setCircularRedirectsAllowed(false)
                .setExpectContinueEnabled(false)
                .setCookieSpec(CookieSpecs.DEFAULT);

        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();

        registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());

        if (config.isIncludeHttpsPages()) {
            try {
                registryBuilder.register("https", new SSLConnectionSocketFactory(SSLSocketUtil.getSslcontext()));
            } catch (Exception e) {
                logger.error("Https Registry Fail : {}", e.toString());
            }
        }

        if (config.getProxyHost() != null) {
            HttpHost proxy = new HttpHost(config.getProxyHost(), config.getProxyPort());
            requestConfigBuilder.setProxy(proxy);

            if (null != config.getProxyUsername()) {
                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(config.getProxyHost(), config.getProxyPort()),
                        new UsernamePasswordCredentials(config.getProxyUsername(), config.getProxyPassword()));
            }
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = registryBuilder.build();

        MessageConstraints messageConstraints = MessageConstraints.custom()
                .setMaxHeaderCount(200)
                .build();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setMalformedInputAction(CodingErrorAction.IGNORE)
                .setUnmappableInputAction(CodingErrorAction.IGNORE)
                .setCharset(Consts.UTF_8)
                .setMessageConstraints(messageConstraints)
                .build();

        connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry, randomDns());
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        connectionManager.setMaxTotal(config.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnectionsPerHost());

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


        defaultHeaders = new ArrayList<>();
        defaultHeaders.add(new BasicHeader(HttpHeaders.CONNECTION, "close"));
        //不使用GZIP。。。部分网站响应GZIP 会导致不返回...
        //defaultHeaders.add(new BasicHeader(HttpHeaders.ACCEPT_ENCODING, "gzip"));

        new IdleConnectionMonitor(connectionManager).start();
        defaultRequestConfig = requestConfigBuilder.build();
        init = true;
    }

    private HttpClient _init() {
        synchronized (lock) {
            return newBasic();
        }
    }

    public CrawlConfig getConfig() {
        return config;
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

    class IdleConnectionMonitor extends Thread {

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
