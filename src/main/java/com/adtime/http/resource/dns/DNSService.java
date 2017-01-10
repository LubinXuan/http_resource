package com.adtime.http.resource.dns;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xuanlubin on 2016/9/11.
 * Dns 查询服务
 */
class DNSService {

    private static final Logger logger = LoggerFactory.getLogger(DNSService.class);

    private static final int CONTEXT_SIZE = 10;

    private static InitialDirContext[] contexts = new InitialDirContext[CONTEXT_SIZE];

    private static AtomicInteger id = new AtomicInteger(0);

    static void init(String[] dnsServer, int timeout) {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns://" + StringUtils.join(dnsServer, " dns://"));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(timeout));
        try {
            for (int i = 0; i < CONTEXT_SIZE; i++) {
                contexts[i] = new InitialDirContext(env);
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    static List<String> search(String type, String address) {

        InitialDirContext context = contexts[Math.abs(id.getAndIncrement() % CONTEXT_SIZE)];

        List<String> resultList = new ArrayList<String>();
        try {
            String dns_attrs[] = {type};
            Attributes attrs = context.getAttributes(address, dns_attrs);
            Attribute attr = attrs.get(type);
            if (attr != null) {
                for (int i = 0; i < attr.size(); i++) {
                    resultList.add((String) attr.get(i));
                }
            }
        } catch (Exception e) {
            logger.warn("获取DNS信息异常::: {}", e.getMessage());
        }
        return resultList;
    }

}
