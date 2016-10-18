package com.adtime.http.resource.http;

import com.adtime.http.resource.CrawlConfig;
import com.adtime.http.resource.WebResource;
import com.adtime.http.resource.dns.DnsCache;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.DnsResolver;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.reactor.IOReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public abstract class HttpClientHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientHelper.class);

    protected CrawlConfig config;

    public CrawlConfig getConfig() {
        return config;
    }

    public void setConfig(CrawlConfig config) {
        this.config = config;
    }

    public abstract void init();

    public abstract RequestConfig requestConfig(Integer connectTimeout, Integer readTimeout);

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
            InetAddress[] addresses = DnsCache.getCacheDns(host);

            if (null == addresses) {
                addresses = InetAddress.getAllByName(host);
                DnsCache.cacheDns(host, addresses);
            }

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

    public abstract HttpClientBuilder createHttpClientBuilder(WebResource webResource);

    public abstract HttpAsyncClientBuilder createHttpAsyncClientBuilder(WebResource webResource) throws IOReactorException;
}
