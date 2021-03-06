package com.adtime.http.resource.extend;

import com.adtime.http.resource.http.HttpClientBaseOperator;
import com.adtime.http.resource.proxy.DynamicProxyProvider;
import com.adtime.http.resource.proxy.ProxyCreator;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Lubin.Xuan on 2016/2/19.
 */
public class DynamicProxyHttpRoutePlanner extends DefaultRoutePlanner {

    private final DynamicProxyProvider proxyProvider;

    private static final ProxyCreator<HttpHost> PROXY_CREATOR = new ProxyCreator<HttpHost>() {

        private Map<String, HttpHost> hostMap = new ConcurrentHashMap<>();

        @Override
        public HttpHost create(DynamicProxyProvider.ProxyInfo proxyInfo) {
            return hostMap.compute(proxyInfo.getHost() + ":" + proxyInfo.getPort(), (s, httpHost) -> {
                if (null == httpHost) {
                    httpHost = new HttpHost(proxyInfo.getHost(), proxyInfo.getPort());
                }
                return httpHost;
            });
        }
    };

    public DynamicProxyHttpRoutePlanner(SchemePortResolver schemePortResolver, DynamicProxyProvider proxyProvider) {
        super(schemePortResolver);
        this.proxyProvider = proxyProvider;
    }

    @Override
    protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        String targetHost = (String) context.getAttribute(HttpClientBaseOperator.HTTP_REAL_HOST);
        String trueHost = null == targetHost ? target.getHostName() : targetHost;
        return proxyProvider.acquireProxy(trueHost, "https".equalsIgnoreCase(target.getSchemeName()), PROXY_CREATOR);
    }
}
