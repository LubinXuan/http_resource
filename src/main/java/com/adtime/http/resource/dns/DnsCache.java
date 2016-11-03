package com.adtime.http.resource.dns;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.util.IPAddressUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xuanlubin on 2016/10/18.
 * 本地DNS缓存 尽量避免 dns解析异常
 */
public class DnsCache {

    private static Random random = new Random();

    private static final Logger logger = LoggerFactory.getLogger(DnsCache.class);

    private static final Map<String, DnsWrap> hostDomainCache = new ConcurrentHashMap<>();

    private static final File DNS_CACHE_STORE_FILE = new File("./dns_local_store.cache");

    private static final AtomicBoolean update = new AtomicBoolean(false);

    protected static void init() {
        try {
            List<String> hostIpLines = FileUtils.readLines(DNS_CACHE_STORE_FILE, "utf-8");
            for (String hostIp : hostIpLines) {
                String[] spilt = StringUtils.split(hostIp, "\t|,");
                if (spilt.length > 1) {
                    InetAddress[] addresses = new InetAddress[spilt.length - 2];
                    for (int i = 1; i < spilt.length - 1; i++) {
                        addresses[i - 1] = InetAddress.getByName(spilt[i]);
                    }
                    DnsWrap dnsWrap = new DnsWrap(addresses, Long.parseLong(spilt[spilt.length - 1]));
                    hostDomainCache.put(spilt[0], dnsWrap);
                    DnsPreFetchUtils.addDnsUpdateTask(spilt[0], dnsWrap.update);
                }
            }
            logger.warn("DNS信息加载完成:{}", hostIpLines.size());
        } catch (IOException e) {
            logger.warn("DNS缓存获取失败!!!");
        }
    }

    public static void storeDnsCacheAsFile() throws IOException {
        if (hostDomainCache.isEmpty()) {
            return;
        }

        if (update.compareAndSet(true, false)) {

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, DnsWrap> entry : hostDomainCache.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(entry.getKey()).append("\t");
                DnsWrap dnsWrap = entry.getValue();
                for (int i = 0; i < dnsWrap.addresses.length; i++) {
                    sb.append(dnsWrap.addresses[i].getHostAddress());
                    if (i + 1 != dnsWrap.addresses.length) {
                        sb.append(",");
                    }
                }
                sb.append("\t").append(dnsWrap.update);
            }

            FileUtils.write(DNS_CACHE_STORE_FILE, sb.toString(), "utf-8");
        }
    }


    public static void cacheDns(String domain, InetAddress[] addresses) {
        if (null == addresses || addresses.length == 0) {
            return;
        }
        hostDomainCache.put(domain.toLowerCase(), new DnsWrap(addresses, System.currentTimeMillis()));
        update.set(true);
    }

    public static InetAddress[] getCacheDns(String host) {
        DnsWrap dnsWrap = hostDomainCache.computeIfPresent(host.toLowerCase(), (s, address) -> {
            logger.info("hit dns from cache :::{}", s);
            return address;
        });
        return null != dnsWrap ? dnsWrap.addresses : null;
    }

    public static boolean contains(String host) {
        return hostDomainCache.containsKey(host.toLowerCase());
    }

    public static InetAddress random(String host) throws UnknownHostException {
        return random(host, false);
    }

    private static final Map<String, Object> DNS_FETCH_LOCK = new ConcurrentHashMap<>();

    public static InetAddress random(String host, boolean sync) throws UnknownHostException {
        if (IPAddressUtil.isIPv4LiteralAddress(host)) {
            return InetAddress.getByName(host);
        }

        InetAddress[] addresses = getCacheDns(host);

        if (null == addresses && sync) {
            final Object lock = DNS_FETCH_LOCK.computeIfAbsent(host, k -> new Object());
            synchronized (lock) {
                if (contains(host)) {
                    addresses = getCacheDns(host);
                } else {
                    addresses = DnsPreFetchUtils.queryDns(host);
                }
            }
        }

        if (null == addresses || addresses.length == 0) {
            throw new UnknownHostException(host);
        } else if (addresses.length == 1) {
            return addresses[0];
        } else {
            shuffle(addresses);
            return addresses[0];
        }
    }

    public static InetAddress randomSync(String host) throws UnknownHostException {
        return random(host, true);
    }


    private static void shuffle(Object[] var0) {
        int var2 = var0.length;
        for (int var3 = var2; var3 > 1; --var3) {
            swap(var0, var3 - 1, random.nextInt(var3));
        }

    }

    private static void swap(Object[] var0, int var1, int var2) {
        Object var3 = var0[var1];
        var0[var1] = var0[var2];
        var0[var2] = var3;
    }

    static class DnsWrap {
        private InetAddress[] addresses;
        private long update;

        DnsWrap(InetAddress[] addresses, long update) {
            this.addresses = addresses;
            this.update = update;
        }

        public InetAddress[] getAddresses() {
            return addresses;
        }

        public long getUpdate() {
            return update;
        }
    }
}
