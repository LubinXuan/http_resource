package com.adtime.http.resource.proxy;

import com.adtime.http.resource.url.URLCanonicalizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
        private String proxyType;
        private String description;

        public ProxyInfo(String host, Integer port, String proxyType, String description) {
            this.host = host;
            this.port = port;
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

        public boolean isSocks() {
            return proxyType.toLowerCase().startsWith("socks");
        }
    }


    public static void setDefaultProxy(String host, int port, String proxyType) {
        setDefaultProxy(new ProxyInfo(host, port, proxyType, "default"));
    }

    public static void setDefaultProxy(ProxyInfo defaultProxy) {
        _default = defaultProxy;
    }


    private List<ProxyInfo> proxyArr = new ArrayList<>();
    private List<ProxyInfo> secureProxyArr = new ArrayList<>();
    private Map<String, ProxyCursor> domainProxyCursorMap = new ConcurrentHashMap<>();


    public void updateProxy(Set<String> proxies) {
        reset(initProxy(proxies));
    }

    private void reset(List<ProxyInfo> proxyInfoList) {
        domainProxyCursorMap.clear();
        List<ProxyInfo> secureProxyArr = new ArrayList<>();
        List<ProxyInfo> proxyArr = new ArrayList<>();
        for (ProxyInfo proxyInfo : proxyInfoList) {
            if (StringUtils.equalsIgnoreCase("https", proxyInfo.getProxyType())) {
                secureProxyArr.add(proxyInfo);
            }
            proxyArr.add(proxyInfo);
        }
        logger.info("代理信息更新 http:{}  secure:{}", proxyArr.size(), secureProxyArr.size());
        this.secureProxyArr = secureProxyArr;
        this.proxyArr = proxyArr;
    }

    private List<ProxyInfo> initProxy(Set<String> proxySet) {
        List<ProxyInfo> proxies = new ArrayList<>();
        for (String proxyStr : proxySet) {
            String[] p = proxyStr.split(":");
            String description = p.length == 4 ? p[3] : null;
            proxies.add(new ProxyInfo(p[1], Integer.parseInt(p[2]), p[0], description));
        }
        return proxies;
    }

    private ExecutorService service = Executors.newFixedThreadPool(20);

    public void filter(int limitTime) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(proxyArr.size() + secureProxyArr.size());
        List<ProxyInfo> proxyInfoList = new ArrayList<>();
        proxyInfoList.addAll(proxyArr);
        proxyInfoList.addAll(secureProxyArr);
        List<ProxyInfo> httpProxy = test(proxyInfoList, limitTime, latch);
        latch.await();
        reset(httpProxy);
    }

    private List<ProxyInfo> test(List<ProxyInfo> proxyInfoArr, int limitTime, CountDownLatch latch) {
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
            List<ProxyInfo> proxyArr = DynamicProxyProvider.this.proxyArr;
            ProxyInfo proxyInfo = null;

            if (proxyArr.size() > idx) {
                proxyInfo = proxyArr.get(idx);
                idx++;
            } else {
                idx = 0;
            }
            return proxyInfo;
        }

        /**
         * 获取https支持代理
         *
         * @return
         */
        public synchronized ProxyInfo getSecure() {
            List<ProxyInfo> proxyArr = secureProxyArr;
            ProxyInfo proxyInfo = null;

            if (proxyArr.size() > secIdx) {
                proxyInfo = proxyArr.get(secIdx);
                secIdx++;
            } else {
                secIdx = 0;
            }
            return proxyInfo;
        }

    }
}
