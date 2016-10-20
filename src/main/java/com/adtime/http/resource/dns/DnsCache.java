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

    public static void cacheDns(String domain, InetAddress[] inetAddresses) {
        if (null == inetAddresses) {
            return;
        }
        hostDomainCache.put(domain.toLowerCase(), inetAddresses);
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

    public static InetAddress random(String host) {
        if (IPAddressUtil.isIPv4LiteralAddress(host)) {
            try {
                return InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        InetAddress[] addresses = getCacheDns(host);
        if (null == addresses || addresses.length == 0) {
            return null;
        } else if (addresses.length == 1) {
            return addresses[0];
        } else {
            shuffle(addresses);
            return addresses[0];
        }
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
