package com.adtime.http.resource.proxy;

import com.adtime.http.resource.url.URLCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Lubin.Xuan on 2016/2/18.
 */
public class DynamicProxyProvider {

    private static final Logger logger = LoggerFactory.getLogger(DynamicProxyProvider.class);

    private static ProxyInfo _default = null;

    public static class ProxyInfo {
        private String host;
        private Integer port;
        private boolean secure;
        private String proxyType;
        private String description;

        public ProxyInfo(String host, Integer port, String proxyType, String description) {
            this.host = host;
            this.port = port;
            this.secure = !"http".equals(proxyType);
            this.proxyType = proxyType;
            this.description = description;
        }

        public String getProxyType() {
            return proxyType;
        }

        public String getDescription() {
            return description;
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

        public boolean isSocks() {
            return proxyType.toLowerCase().startsWith("socks");
        }
    }


    public static void setDefaultProxy(String host, int port) {
        _default = new ProxyInfo(host, port, "http", "default");
    }

    public static void setDefaultProxy(ProxyInfo defaultProxy) {
        _default = defaultProxy;
    }


    private ProxyInfo[] proxyArr = new ProxyInfo[0];
    private ProxyInfo[] secureProxyArr = new ProxyInfo[0];
    private Map<String, ProxyCursor> domainProxyCursorMap = new ConcurrentHashMap<>();


    public void updateProxy(Set<String> httpProxySet, Set<String> httpsProxySet) {
        ProxyInfo[] _httpProxyArr = initProxy(httpProxySet);
        ProxyInfo[] _httpsProxyArr = initProxy(httpsProxySet);
        reset(_httpProxyArr, _httpsProxyArr);
    }

    private void reset(ProxyInfo[] _httpProxyArr, ProxyInfo[] _httpsProxyArr) {
        domainProxyCursorMap.clear();
        proxyArr = _httpProxyArr;
        secureProxyArr = _httpsProxyArr;
        logger.info("代理信息更新 http:{}  secure:{}", proxyArr.length, secureProxyArr.length);
    }

    private ProxyInfo[] initProxy(Set<String> proxySet) {
        ProxyInfo[] proxies = new ProxyInfo[proxySet.size()];
        int idx = 0;
        for (String proxyStr : proxySet) {
            String[] p = proxyStr.split(":");
            String description = p.length == 4 ? p[3] : null;
            proxies[idx] = new ProxyInfo(p[1], Integer.parseInt(p[2]), p[0], description);
            idx++;
        }
        return proxies;
    }

    private ExecutorService service = Executors.newFixedThreadPool(20);

    public void filter(int limitTime) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(proxyArr.length + secureProxyArr.length);
        List<ProxyInfo> httpProxy = test(proxyArr, limitTime, latch);
        List<ProxyInfo> httpsProxy = test(secureProxyArr, limitTime, latch);
        latch.await();
        reset(httpProxy.toArray(new ProxyInfo[httpProxy.size()]), httpsProxy.toArray(new ProxyInfo[httpsProxy.size()]));
    }

    private List<ProxyInfo> test(ProxyInfo[] proxyInfoArr, int limitTime, CountDownLatch latch) {
        List<ProxyInfo> httpProxyList = new LinkedList<>();
        for (ProxyInfo proxyInfo : proxyInfoArr) {
            service.execute(() -> {
                int testTime = ProxyRateTest.test(proxyInfo.getHost());
                if (testTime > 0 && testTime < limitTime) {
                    httpProxyList.add(proxyInfo);
                } else {
                    logger.debug("代理连接速度过慢  {} {}:{} {} {}", proxyInfo.getProxyType(), proxyInfo.getHost(), proxyInfo.getPort(), testTime, proxyInfo.getDescription());
                }
                latch.countDown();
            });
        }
        return httpProxyList;
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

        logger.debug("代理信息  {}->[{} {}:{}] {}", host, proxyInfo.getProxyType(), proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getDescription());

        return proxyCreator.create(proxyInfo);
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
            ProxyInfo[] proxyArr = DynamicProxyProvider.this.proxyArr;
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
            ProxyInfo[] proxyArr = secureProxyArr;
            ProxyInfo proxyInfo = null;

            if (proxyArr.length > idx) {
                proxyInfo = proxyArr[idx];
                idx++;
            } else {
                idx = 0;
            }
            return proxyInfo;
        }

    }
}
