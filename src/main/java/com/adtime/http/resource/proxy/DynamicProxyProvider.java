package com.adtime.http.resource.proxy;

import com.adtime.http.resource.url.URLCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Lubin.Xuan on 2016/2/18.
 */
public class DynamicProxyProvider {

    private static final Logger logger = LoggerFactory.getLogger(DynamicProxyProvider.class);

    private static ProxyInfo _default = null;

    static class ProxyInfo {
        private String host;
        private Integer port;
        private boolean secure;

        public ProxyInfo(String host, Integer port, boolean secure) {
            this.host = host;
            this.port = port;
            this.secure = secure;
        }

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }

        public boolean isSecure() {
            return secure;
        }
    }


    public static void setDefaultProxy(String host, int port) {
        _default = new ProxyInfo(host, port, false);
    }


    private ProxyInfo[] httpProxyArr = new ProxyInfo[0];
    private ProxyInfo[] httpsProxyArr = new ProxyInfo[0];
    private Map<String, ProxyCursor> domainProxyCursorMap = new ConcurrentHashMap<>();


    public void updateProxy(Set<String> httpProxySet, Set<String> httpsProxySet) {
        ProxyInfo[] _httpProxyArr = initProxy(httpProxySet);
        ProxyInfo[] _httpsProxyArr = initProxy(httpsProxySet);
        domainProxyCursorMap.clear();
        httpProxyArr = _httpProxyArr;
        httpsProxyArr = _httpsProxyArr;
        logger.info("代理信息更新 http:{}  https:{}", httpProxyArr.length, httpsProxyArr.length);
    }

    private ProxyInfo[] initProxy(Set<String> proxySet) {
        ProxyInfo[] proxies = new ProxyInfo[proxySet.size()];
        int idx = 0;
        for (String proxyStr : proxySet) {
            String[] p = proxyStr.split(":");
            proxies[idx] = new ProxyInfo(p[1], Integer.parseInt(p[2]), "https".equals(p[0]));
            idx++;
        }
        return proxies;
    }

    public <T> T acquireProxy(String url, ProxyCreator<T> proxyCreator) {

        String host = URLCanonicalizer.getHost(url);

        boolean secure = url.toLowerCase().startsWith("https://");

        return acquireProxy(host, secure, proxyCreator);
    }

    public <T> T acquireProxy(String host, boolean secure, ProxyCreator<T> proxyCreator) {

        ProxyCursor cursor = domainProxyCursorMap.compute(host, (s, cursor1) -> {
            if (null == cursor1) {
                cursor1 = new ProxyCursor();
            }
            return cursor1;
        });

        ProxyInfo proxyInfo;

        if (secure) {
            proxyInfo = cursor.getSecure();
        } else {
            proxyInfo = cursor.get();
            if (null == proxyInfo) {
                proxyInfo = cursor.getSecure();
            }
        }


        if (null == proxyInfo) {
            if (null != _default) {
                proxyInfo = _default;
            } else {
                return null;
            }
        }

        logger.debug("代理信息  {}->[{}:{}]", host, proxyInfo.getHost(), proxyInfo.getPort());

        return proxyCreator.create(proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.isSecure());
    }

    private class ProxyCursor {
        private int idx = 0;
        private int secIdx = 0;

        /**
         * 获取http代理
         *
         * @return
         */
        public synchronized ProxyInfo get() {
            ProxyInfo[] proxyArr = httpProxyArr;
            ProxyInfo proxyInfo = null;

            if (proxyArr.length > secIdx) {
                proxyInfo = proxyArr[secIdx];
                secIdx++;
            } else {
                secIdx = 0;
            }
            return proxyInfo;
        }

        /**
         * 获取https支持代理
         *
         * @return
         */
        public synchronized ProxyInfo getSecure() {
            ProxyInfo[] proxyArr = httpsProxyArr;
            ProxyInfo proxyInfo = null;

            if (proxyArr.length > idx) {
                proxyInfo = proxyArr[secIdx];
                idx++;
            } else {
                idx = 0;
            }
            return proxyInfo;
        }

    }
}
