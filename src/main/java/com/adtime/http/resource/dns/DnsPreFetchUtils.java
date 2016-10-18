package com.adtime.http.resource.dns;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import sun.net.util.IPAddressUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by xuanlubin on 2016/9/8.
 */
public class DnsPreFetchUtils {

    private static final ConcurrentHashSet<String> DOMAIN_FILTER = new ConcurrentHashSet<>();

    private static final List<DnsUpdateInfo> DOMAIN_FETCH_QUEUE = new ArrayList<>(65535);

    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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

    private static void updateDnsInfo(DnsUpdateInfo updateInfo, boolean sync) {

        Runnable runnable = () -> {
            if (updateInfo.createTime > System.currentTimeMillis() - UPDATE_REQUIRE_TIME / 2) {
                return;
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
                    DnsJavaNameServiceDescriptor.cacheDns(updateInfo.domain, inetAddresses);
                    break;
                }
            }
        };

        if (sync) {
            Future future = SERVICE.submit(runnable);
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            SERVICE.execute(runnable);
        }
    }


    public static void preFetch(String domain) {

        if (StringUtils.isBlank(domain)) {
            return;
        }

        if (IPAddressUtil.isIPv4LiteralAddress(domain)) {
            return;
        }

        if (DOMAIN_FILTER.add(domain.toLowerCase())) {
            DnsUpdateInfo dnsUpdateInfo = new DnsUpdateInfo();
            dnsUpdateInfo.domain = domain;
            dnsUpdateInfo.createTime = -1;
            DOMAIN_FETCH_QUEUE.add(dnsUpdateInfo);
            updateDnsInfo(dnsUpdateInfo, true);
        }
    }

}
