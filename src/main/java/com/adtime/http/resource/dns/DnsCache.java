package com.adtime.http.resource.dns;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2016/10/18.
 */
public class DnsCache {
    private static final Map<String, InetAddress[]> hostDomainCache = new ConcurrentHashMap<>();

    public static void cacheDns(String domain, InetAddress[] inetAddresses) {
        if (null == inetAddresses) {
            return;
        }
        hostDomainCache.put(domain.toLowerCase(), inetAddresses);
    }

    public static InetAddress[] getCacheDns(String host) {
        return hostDomainCache.get(host.toLowerCase());
    }

    public static boolean contains(String host) {
        return hostDomainCache.containsKey(host);
    }

}
