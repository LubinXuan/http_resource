package com.adtime.http.resource.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2016/10/18.
 */
public class DnsCache {

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

}
