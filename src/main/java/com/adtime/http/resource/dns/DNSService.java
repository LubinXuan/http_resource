package com.adtime.http.resource.dns;

import org.apache.commons.lang3.StringUtils;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by xuanlubin on 2016/9/11.
 * Dns 查询服务
 */
public class DNSService {

    private static InitialDirContext context = null;

    public static void init(String[] dnsServer, int timeout) {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns://" + StringUtils.join(dnsServer, " dns://"));
        env.put("com.sun.jndi.ldap.read.timeout", String.valueOf(timeout));
        try {
            context = new InitialDirContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    public static List<String> search(String type, String address) {

        if (null == context) {
            return Collections.emptyList();
        }

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
            e.printStackTrace();
        }
        return resultList;
    }

}
