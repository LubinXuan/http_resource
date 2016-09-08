package com.adtime.http.resource.dns;

import org.apache.commons.io.IOUtils;
import org.xbill.DNS.Address;
import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by xuanlubin on 2016/9/8.
 */
public class DnsJavaNameServiceDescriptor implements NameServiceDescriptor {

    @Override
    public NameService createNameService() throws Exception {
        return new NameService() {
            @Override
            public InetAddress[] lookupAllHostAddr(String s) throws UnknownHostException {
                return Address.getAllByName(s);
            }

            @Override
            public String getHostByAddr(byte[] bytes) throws UnknownHostException {
                System.out.println("-------------"+new String(bytes));
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
}
