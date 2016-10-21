package com.adtime.http.resource.url;

import com.adtime.http.resource.dns.DnsCache;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URL;

/**
 * Created by xuanlubin on 2016/10/21.
 */
public class URLInetAddress {

    private static Field URL_AUTHORITY_FIELD = null;

    static {
        try {
            URL_AUTHORITY_FIELD = URL.class.getDeclaredField("authority");
            URL_AUTHORITY_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    //protocol://hist:port/path?query#hash
    public static URL create(String targetUrl) throws Exception {
        URL url = new URL(targetUrl);
        InetAddress inetAddress = DnsCache.randomSync(url.getHost());
        if (null != inetAddress && !StringUtils.equalsIgnoreCase(inetAddress.getHostAddress(), url.getHost())) {
            URL_AUTHORITY_FIELD.set(url, inetAddress.getHostAddress());
        }
        return url;
    }
}
