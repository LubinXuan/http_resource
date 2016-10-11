package com.adtime.http.resource.dns;

import org.xbill.DNS.spi.DNSJavaNameServiceDescriptor;
import sun.net.spi.nameservice.NameService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2016/9/8.
 */
public class DnsJavaNameServiceDescriptor extends DNSJavaNameServiceDescriptor {

    private static final Map<String, InetAddress[]> hostDomainCache = new ConcurrentHashMap<>();

    @Override
    public NameService createNameService() {
        return new NameService() {

            NameService superNs = DnsJavaNameServiceDescriptor.this.createNameService();

            @Override
            public InetAddress[] lookupAllHostAddr(String s) throws UnknownHostException {
                return hostDomainCache.getOrDefault(s.toLowerCase(), superNs.lookupAllHostAddr(s));
            }

            @Override
            public String getHostByAddr(byte[] bytes) throws UnknownHostException {
                System.out.println("-------------" + new String(bytes));
                return superNs.getHostByAddr(bytes);
            }
        };
    }

    @Override
    public String getProviderName() {
        return "xbill";
    }

    @Override
    public String getType() {
        return "dns";
    }

    public static void cacheDns(String domain, InetAddress[] inetAddresses) {
        hostDomainCache.put(domain.toLowerCase(), inetAddresses);
    }
}
