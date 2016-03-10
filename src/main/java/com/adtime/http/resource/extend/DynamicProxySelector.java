package com.adtime.http.resource.extend;

import com.adtime.http.resource.proxy.DynamicProxyProvider;
import com.adtime.http.resource.proxy.ProxyCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Lubin.Xuan on 2016/2/19.
 */
public class DynamicProxySelector extends ProxySelector {

    private static final Logger logger = LoggerFactory.getLogger(DynamicProxySelector.class);

    private ProxySelector defaultSelector;

    private static final ProxyCreator<Proxy> PROXY_CREATOR = new ProxyCreator<Proxy>() {

        private Map<String, Proxy> proxyMap = new ConcurrentHashMap<>();

        @Override
        public Proxy create(DynamicProxyProvider.ProxyInfo proxyInfo) {

            if (proxyInfo.isSecure() && !proxyInfo.isSocks()) {
                return Proxy.NO_PROXY;
            }

            return proxyMap.compute(proxyInfo.getHost() + ":" + proxyInfo.getPort() + ":" + proxyInfo.isSecure(), (s, httpHost) -> {
                if (null == httpHost) {
                    httpHost = new Proxy(proxyInfo.isSocks() ? Proxy.Type.SOCKS : Proxy.Type.HTTP, new InetSocketAddress(proxyInfo.getHost(), proxyInfo.getPort()));
                }
                return httpHost;
            });
        }
    };

    private final DynamicProxyProvider proxyProvider;

    public DynamicProxySelector(DynamicProxyProvider proxyProvider) {
        this.proxyProvider = proxyProvider;
        this.defaultSelector = getDefault();
    }

    @Override
    public List<Proxy> select(URI uri) {
        //避免HttpClient重复连接代理
        if (null == proxyProvider || "socket".equalsIgnoreCase(uri.getScheme())) {
            return defaultSelector.select(uri);
        }
        Proxy proxy = proxyProvider.acquireProxy(uri.getHost(), "https".equalsIgnoreCase(uri.getScheme()), PROXY_CREATOR);
        if (null != proxy) {
            return Collections.singletonList(proxy);
        } else {
            return defaultSelector.select(uri);
        }
    }

    @Override
    public void connectFailed(URI uri, SocketAddress socketAddress, IOException e) {
        logger.error("{} {} {}", uri, socketAddress, e.toString());
    }
}
