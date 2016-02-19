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
        public Proxy create(String host, Integer port, boolean secure) {

            if (secure) {
                return Proxy.NO_PROXY;
            }

            return proxyMap.compute(host +":"+ port +":"+ secure, (s, httpHost) -> {
                if (null == httpHost) {
                    httpHost = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
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
