package com.adtime.http.resource.dns;

import com.adtime.http.ShutdownHook;
import com.adtime.http.resource.ConnectionAbortUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.util.IPAddressUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xuanlubin on 2016/9/8.
 */
public class DnsPreFetchUtils {

    private static final Logger logger = LoggerFactory.getLogger(DnsPreFetchUtils.class);

    private static final ConcurrentHashSet<String> DOMAIN_FILTER = new ConcurrentHashSet<>();

    private static final BlockingQueue<DnsUpdateInfo> DOMAIN_FETCH_QUEUE = new LinkedBlockingQueue<>(65535);

    private static final Map<String, AtomicInteger> ERROR_DNS_FETCH_COUNT = new ConcurrentHashMap<>();

    private static final long UPDATE_REQUIRE_TIME = 900000;

    private static class DnsUpdateInfo {
        private String domain;
        private long createTime;
    }

    static {

        String dnsServers = System.getProperty("dns.server", "114.114.114.114,8.8.8.8");
        String[] _dnsServers = dnsServers.split(",");


        DnsCache.init();

        DNSService.init(_dnsServers, 2000);

        AtomicInteger count = new AtomicInteger(0);

        AtomicBoolean block = new AtomicBoolean(false);

        ConnectionAbortUtils.register(new ConnectionAbortUtils.ConnectionAbort() {
            @Override
            public void onAbort() {
                block.set(true);
            }

            @Override
            public void onStable() {
                block.set(false);
            }
        });


        Thread updateDnsThread = new Thread(() -> {
            ShutdownHook shutdownHook = new ShutdownHook();
            while (!shutdownHook.isShutdown()) {
                DnsUpdateInfo info;
                try {
                    info = DOMAIN_FETCH_QUEUE.take();
                } catch (InterruptedException e) {
                    continue;
                }

                try {
                    updateDnsAndGet(info);
                    if (count.compareAndSet(100, 0)) {
                        try {
                            DnsCache.storeDnsCacheAsFile();
                        } catch (IOException e) {
                            logger.error("DNS缓存文件化异常", e);
                        }
                    } else {
                        count.incrementAndGet();
                    }
                } finally {
                    DOMAIN_FETCH_QUEUE.offer(info);
                }
            }
        });

        updateDnsThread.setName("DnsInfoUpdateThread");
        updateDnsThread.start();
    }

    private static InetAddress[] updateDnsAndGet(DnsUpdateInfo updateInfo) {
        if (updateInfo.createTime > System.currentTimeMillis() - UPDATE_REQUIRE_TIME / 2) {
            return DnsCache.getCacheDns(updateInfo.domain);
        }
        AtomicInteger count = ERROR_DNS_FETCH_COUNT.get(updateInfo.domain);
        if (null != count && count.get() > 10) {
            updateInfo.createTime = System.currentTimeMillis();
            ERROR_DNS_FETCH_COUNT.remove(updateInfo.domain);
            return null;
        }

        InetAddress[] addresses = queryDns(updateInfo.domain);
        if (null != addresses && addresses.length > 0) {
            updateInfo.createTime = System.currentTimeMillis();
        } else {
            updateInfo.createTime += 1000;//
        }
        return addresses;
    }


    public static InetAddress[] queryDns(String host) {

        if (IPAddressUtil.isIPv4LiteralAddress(host)) {
            try {
                return InetAddress.getAllByName(host);
            } catch (UnknownHostException e) {
                return null;
            }
        }

        host = host.toLowerCase();

        try {
            List<String> resultList = DNSService.search("A", host);
            if (!resultList.isEmpty()) {
                InetAddress[] addresses = new InetAddress[resultList.size()];
                for (int i = 0; i < resultList.size(); i++) {
                    try {
                        addresses[i] = InetAddress.getByName(resultList.get(i));
                    } catch (UnknownHostException ignore) {
                    }
                }
                DnsCache.cacheDns(host, addresses);
                return addresses;
            } else {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                if (null != addresses) {
                    DnsCache.cacheDns(host, addresses);
                    return addresses;
                }
            }
            logger.error("Can't get dns info of [{}]", host);
        } catch (Throwable ignore) {
            logger.warn("DNS 信息获取异常 {}", ignore.toString());
            ERROR_DNS_FETCH_COUNT.computeIfAbsent(host, h -> new AtomicInteger(0)).incrementAndGet();
        }
        return null;
    }


    public static void preFetch(String domain) {
        _preFetch(domain, false);
    }

    public static InetAddress[] _preFetch(String domain, boolean sync) {

        if (StringUtils.isBlank(domain)) {
            return null;
        }

        if (IPAddressUtil.isIPv4LiteralAddress(domain)) {
            try {
                return InetAddress.getAllByName(domain);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        if (DOMAIN_FILTER.add(domain.toLowerCase())) {
            DnsUpdateInfo dnsUpdateInfo = new DnsUpdateInfo();
            dnsUpdateInfo.domain = domain;
            dnsUpdateInfo.createTime = -1;
            DOMAIN_FETCH_QUEUE.offer(dnsUpdateInfo);
            if (sync) {
                return updateDnsAndGet(dnsUpdateInfo);
            }
        }
        return DnsCache.getCacheDns(domain);
    }

    public static void addDnsUpdateTask(String host, Long lastUpdateTime) {

        if (IPAddressUtil.isIPv4LiteralAddress(host)) {
            return;
        }

        if (DOMAIN_FILTER.add(host.toLowerCase())) {
            DnsUpdateInfo dnsUpdateInfo = new DnsUpdateInfo();
            dnsUpdateInfo.domain = host;
            dnsUpdateInfo.createTime = null == lastUpdateTime ? -1 : lastUpdateTime;
            DOMAIN_FETCH_QUEUE.offer(dnsUpdateInfo);
        }
    }

}
