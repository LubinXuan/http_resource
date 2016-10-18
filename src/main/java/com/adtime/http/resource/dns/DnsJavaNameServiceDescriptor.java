package com.adtime.http.resource.dns;

import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;
import sun.net.spi.nameservice.dns.DNSNameService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2016/9/8.
 */
public class DnsJavaNameServiceDescriptor implements NameServiceDescriptor {

    private final NameService superNs;

    public DnsJavaNameServiceDescriptor() throws Exception {
        superNs = new DNSNameService();
    }

    @Override
    public NameService createNameService() throws Exception {
        return new NameService() {

            @Override
            public InetAddress[] lookupAllHostAddr(String s) throws UnknownHostException {
                String host = s.toLowerCase();
                if (!DnsCache.contains(host)) {
                    DnsCache.cacheDns(host, superNs.lookupAllHostAddr(s));
                }
                return DnsCache.getCacheDns(host);
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
}
