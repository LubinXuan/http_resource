package com.adtime.http.resource.http;

import com.adtime.http.resource.CrawlConfig;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.DnsResolver;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public abstract class HttpClientHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientHelper.class);

    public abstract HttpClient basic();

    public abstract HttpClient newBasic();

    public abstract CrawlConfig getConfig();

    public abstract void registerCookie(String domain, String name, String value);

    public static ClientCookie newClientCookie(String domain, String name, String value) {
        BasicClientCookie cookie = new BasicClientCookie(name, value);
        cookie.setDomain(domain);
        cookie.setAttribute(ClientCookie.DOMAIN_ATTR, domain);
        return cookie;
    }

    private static final DnsResolver RANDOM_DNS_RESOLVER = new DnsResolver() {

        private Random random = new Random();

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            InetAddress[] _addresses = new InetAddress[addresses.length];
            if (addresses.length > 1) {
                int idx = random.nextInt(addresses.length);
                if (idx != 0) {
                    _addresses[0] = addresses[idx];
                    if (_addresses.length > 0) {
                        for (int i = 1; i < _addresses.length; i++) {
                            if (idx == i) {
                                _addresses[i] = addresses[0];
                            } else {
                                _addresses[i] = addresses[i - 1];
                            }
                        }
                    }
                } else {
                    _addresses = addresses;
                }
            } else {
                _addresses = addresses;
            }
            logger.debug("Dns prefer sort:{}{}", _addresses, "");
            return _addresses;
        }
    };

    public static DnsResolver randomDns() {
        return RANDOM_DNS_RESOLVER;
    }
}
