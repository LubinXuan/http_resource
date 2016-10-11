package com.adtime.crawl.queue.center;

import sun.net.util.IPAddressUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Lubin.Xuan on 2015/7/22.
 * ie.
 */
public class DomainUtil {

    private static final String tldNamesFileName = "parallel/config/tld-names.txt";
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

    public static String parse(String urlString) {

        try {
            final URL url = new URL(urlString);
            String domain = url.getHost();
            if (IPAddressUtil.isIPv4LiteralAddress(domain)) {
                return domain;
            }
            final String[] parts = domain.split("\\.");
            if (parts.length > 2) {
                domain = parts[parts.length - 2] + "." + parts[parts.length - 1];
                if (tldSet.contains(domain)) {
                    domain = parts[parts.length - 3] + "." + domain;
                }
            }
            return domain;
        } catch (Exception var4) {
            return null;
        }
    }
}
