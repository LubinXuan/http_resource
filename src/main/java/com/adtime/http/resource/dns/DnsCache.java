package com.adtime.http.resource.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.util.IPAddressUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2016/10/18.
 * 本地DNS缓存 尽量避免 dns解析异常
 */
public class DnsCache {

    private static Random random = new Random();

    private static final Logger logger = LoggerFactory.getLogger(DnsCache.class);

    private static final Map<String, InetAddress[]> hostDomainCache = new ConcurrentHashMap<>();

    public static void cacheDns(String domain, InetAddress[] addresses) {
        if (null == addresses || addresses.length == 0) {
            return;
        }
        hostDomainCache.put(domain.toLowerCase(), addresses);
    }

    public static InetAddress[] getCacheDns(String host) {
        return hostDomainCache.computeIfPresent(host.toLowerCase(), (s, address) -> {
            logger.info("hit dns from cache :::{}", s);
            return address;
        });
    }

    public static boolean contains(String host) {
        return hostDomainCache.containsKey(host.toLowerCase());
    }

    public static InetAddress random(String host) throws UnknownHostException {
        return random(host, false);
    }

    private static final Map<String, Object> DNS_FETCH_LOCK = new ConcurrentHashMap<>();

    public static InetAddress random(String host, boolean sync) throws UnknownHostException {
        if (IPAddressUtil.isIPv4LiteralAddress(host)) {
            return InetAddress.getByName(host);
        }

        InetAddress[] addresses = getCacheDns(host);

        if (null == addresses && sync) {
            final Object lock = DNS_FETCH_LOCK.computeIfAbsent(host, k -> new Object());
            synchronized (lock) {
                if (contains(host)) {
                    addresses = getCacheDns(host);
                } else {
                    addresses = DnsPreFetchUtils.queryDns(host);
                }
            }
        }

        if (null == addresses || addresses.length == 0) {
            throw new UnknownHostException(host);
        } else if (addresses.length == 1) {
            return addresses[0];
        } else {
            shuffle(addresses);
            return addresses[0];
        }
    }

    public static InetAddress randomSync(String host) throws UnknownHostException {
        return random(host, true);
    }


    private static void shuffle(Object[] var0) {
        int var2 = var0.length;
        for (int var3 = var2; var3 > 1; --var3) {
            swap(var0, var3 - 1, random.nextInt(var3));
        }

    }

    private static void swap(Object[] var0, int var1, int var2) {
        Object var3 = var0[var1];
        var0[var1] = var0[var2];
        var0[var2] = var3;
    }
}
