package com.adtime.crawl.queue.center;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.util.IPAddressUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Lubin.Xuan on 2015/7/22.
 * ie.
 */
public class DomainUtil {

    private static final Logger logger = LoggerFactory.getLogger(DomainUtil.class);

    private static final String tldNamesFileName = "parallel.config/tld-names.txt";
    private static Set<String> tldSet;


    static {
        tldSet = new HashSet<String>();
        try {
            final InputStream stream = DomainUtil.class.getClassLoader().getResourceAsStream(tldNamesFileName);
            if (stream == null) {
                System.err.println("Couldn't find tld-names.txt");
                System.exit(-1);
            }
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (line.startsWith("//")) {
                        continue;
                    }
                    tldSet.add(line);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] parse(String urlString) {

        try {
            urlString = StringUtils.substringAfter(urlString, "://");
            int end = StringUtils.indexOfAny(urlString, "/", "?", "#");

            String domain, host;
            if (end > 0) {
                host = StringUtils.substring(urlString, 0, end);
            } else {
                host = urlString;
            }
            domain = host;
            if (IPAddressUtil.isIPv4LiteralAddress(host)) {
                return new String[]{domain, host};
            }
            final String[] parts = host.split("\\.");
            if (parts.length > 2) {
                domain = parts[parts.length - 2] + "." + parts[parts.length - 1];
                if (tldSet.contains(domain)) {
                    domain = parts[parts.length - 3] + "." + domain;
                }
            }
            return new String[]{domain, host};
        } catch (Exception var4) {
            logger.warn("domain,host 解析失败  {}", urlString);
            throw var4;
        }
    }

    public static void main(String[] args) {
        parse("http://www.baidu.com?SSS");
    }
}
