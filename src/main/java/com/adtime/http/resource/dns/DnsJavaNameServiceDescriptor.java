package com.adtime.http.resource.dns;

import org.xbill.DNS.Address;
import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2016/9/8.
 */
public class DnsJavaNameServiceDescriptor implements NameServiceDescriptor {

    private static final Map<String, InetAddress[]> hostDomainCache = new ConcurrentHashMap<>();

    @Override
    public NameService createNameService() throws Exception {
        return new NameService() {
            @Override
            public InetAddress[] lookupAllHostAddr(String s) throws UnknownHostException {
                return hostDomainCache.getOrDefault(s.toLowerCase(), Address.getAllByName(s));
            }

            @Override
            public String getHostByAddr(byte[] bytes) throws UnknownHostException {
                System.out.println("-------------" + new String(bytes));
                return "";
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
