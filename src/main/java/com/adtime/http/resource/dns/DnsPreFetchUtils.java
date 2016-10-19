package com.adtime.http.resource.dns;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.util.IPAddressUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by xuanlubin on 2016/9/8.
 */
public class DnsPreFetchUtils {

    private static final Logger logger = LoggerFactory.getLogger(DnsPreFetchUtils.class);

    private static final ConcurrentHashSet<String> DOMAIN_FILTER = new ConcurrentHashSet<>();

    private static final List<DnsUpdateInfo> DOMAIN_FETCH_QUEUE = new ArrayList<>(65535);

    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(3);

    private static List<String> NAME_SERVERS = new ArrayList<>();

    private static final long UPDATE_REQUIRE_TIME = 60000;

    private static class DnsUpdateInfo {
        private String domain;
        private long createTime;
    }

    static {

        String dnsServers = System.getProperty("dns.server", "114.114.114.114");
        String[] _dnsServers = dnsServers.split(",");
        Collections.addAll(NAME_SERVERS, _dnsServers);

        new Timer("DnsInfoUpdate").schedule(new TimerTask() {
            @Override
            public void run() {
                for (Iterator<DnsUpdateInfo> iterator = DOMAIN_FETCH_QUEUE.iterator(); iterator.hasNext(); ) {
                    DnsUpdateInfo updateInfo = iterator.next();
                    if (updateInfo.createTime < System.currentTimeMillis() - UPDATE_REQUIRE_TIME) {
                        updateDnsInfo(updateInfo, false);
                    }
                }
            }
        }, 5000, 5000);
    }

    private static InetAddress[] updateDnsInfo(DnsUpdateInfo updateInfo, boolean sync) {

        Callable<InetAddress[]> runnable = () -> {
            if (updateInfo.createTime > System.currentTimeMillis() - UPDATE_REQUIRE_TIME / 2) {
                return DnsCache.getCacheDns(updateInfo.domain);
            }
            for (String nameServer : NAME_SERVERS) {
                List<String> resultList = DNSService.search(nameServer, 2000, "A", updateInfo.domain);
                if (!resultList.isEmpty()) {
                    InetAddress[] inetAddresses = new InetAddress[resultList.size()];
                    for (int i = 0; i < resultList.size(); i++) {
                        try {
                            inetAddresses[i] = InetAddress.getByName(resultList.get(i));
                        } catch (UnknownHostException ignore) {
                        }
                    }
                    updateInfo.createTime = System.currentTimeMillis();
                    DnsCache.cacheDns(updateInfo.domain, inetAddresses);
                    return inetAddresses;
                }
            }

            logger.error("Can't get dns info of [{}]", updateInfo.domain);

            return null;
        };

        Future<InetAddress[]> future = SERVICE.submit(runnable);

        if (sync) {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static void preFetch(String domain) {
        _preFetch(domain, false);
    }

    public static InetAddress[] _preFetch(String domain, boolean sync) {

        if (StringUtils.isBlank(domain)) {
            return null;
        }

        if (IPAddressUtil.isIPv4LiteralAddress(domain)) {
            try {
                return InetAddress.getAllByName(domain);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        if (DOMAIN_FILTER.add(domain.toLowerCase())) {
            DnsUpdateInfo dnsUpdateInfo = new DnsUpdateInfo();
            dnsUpdateInfo.domain = domain;
            dnsUpdateInfo.createTime = -1;
            DOMAIN_FETCH_QUEUE.add(dnsUpdateInfo);
            return updateDnsInfo(dnsUpdateInfo, sync);
        } else {
            return DnsCache.getCacheDns(domain);
        }
    }

    public static InetAddress[] preFetchSync(String domain) {
        return _preFetch(domain, true);
    }

}
