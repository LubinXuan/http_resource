package com.adtime.http.resource.url;

import com.adtime.http.resource.dns.DnsCache;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Created by xuanlubin on 2016/10/21.
 */
public class URLInetAddress {

    private static Field URL_AUTHORITY_FIELD = null;

    private static boolean HOST_REPLACE = true;

    static {
        try {
            URL_AUTHORITY_FIELD = URL.class.getDeclaredField("authority");
            URL_AUTHORITY_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void disableHostReplace() {
        HOST_REPLACE = false;
    }

    //protocol://hist:port/path?query#hash
    public static URL create(String targetUrl) throws MalformedURLException, UnknownHostException {
        URL url = new URL(targetUrl);
        if (HOST_REPLACE) {
            InetAddress inetAddress = DnsCache.randomSync(url.getHost());
            if (null != inetAddress && !StringUtils.equalsIgnoreCase(inetAddress.getHostAddress(), url.getHost())) {
                try {
                    URL_AUTHORITY_FIELD.set(url, inetAddress.getHostAddress());
                } catch (Exception ignore) {

                }
            }
        }
        return url;
    }


}
