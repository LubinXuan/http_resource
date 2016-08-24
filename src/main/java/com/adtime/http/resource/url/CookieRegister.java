package com.adtime.http.resource.url;

import com.adtime.http.resource.WebResource;
import com.adtime.http.resource.common.ConfigReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Lubin.Xuan on 2015/6/12.
 * ie.
 */
public class CookieRegister {

    private static final Logger logger = LoggerFactory.getLogger(CookieRegister.class);

    private static final File STORE_FILE = new File("hostCookieDisable.cfg");

    private List<WebResource> webResourceList;

    private String cookieConf;

    private Set<String> hostFilter = new HashSet<>();

    @PostConstruct
    public void init() throws IOException {
        try {
            String data = ConfigReader.readNOE(cookieConf);
            String[] lines = data.split("\n");
            Set<String> cookie = new HashSet<>();
            for (String line : lines) {
                String[] ck = line.split("\\\\t");
                if (ck.length == 3 && cookie.add(ck[1] + "@" + ck[0])) {
                    registerCookie(ck[1], ck[0], ck[2]);
                }
            }
        } catch (Exception e) {
            logger.error("默认Cookie初始化出错", e);
        }
        if (!STORE_FILE.exists() || STORE_FILE.isDirectory()) {
            return;
        }
        List<String> lines = FileUtils.readLines(STORE_FILE);
        for (String host : lines) {
            if (StringUtils.startsWith(host, "#")) {
                continue;
            }
            _disableHostCookie(host, false);
        }
    }


    public void setCookieConf(String cookieConf) {
        this.cookieConf = cookieConf;
    }

    public void setWebResourceList(List<WebResource> webResourceList) {
        this.webResourceList = webResourceList;
    }


    public void registerCookie(String domain, String name, String value) {
        if (null == webResourceList || webResourceList.isEmpty()) {
            return;
        }
        webResourceList.forEach(w -> w.registerCookie(domain, name, value));
    }

    public void disableHostCookie(String host) {
        _disableHostCookie(host, true);
    }

    public void enbleHostCookie(String host) {
        if (!hostFilter.remove(host)) {
            return;
        }
        save();
        if (null == webResourceList || webResourceList.isEmpty()) {
            return;
        }
        logger.info("启用Cookie Host:{}", host);
        webResourceList.forEach(w -> w.enableCookieSupport(host));

    }

    public void _disableHostCookie(String host, boolean save) {
        if (!hostFilter.add(host)) {
            return;
        }
        if (save) {
            save();
        }
        if (null == webResourceList || webResourceList.isEmpty()) {
            return;
        }

        logger.info("禁用Cookie Host:{}", host);

        webResourceList.forEach(w -> w.disableCookieSupport(host));
    }

    private void save() {
        String data = StringUtils.join(hostFilter, "\n");
        try {
            FileUtils.write(STORE_FILE, data, "utf-8");
        } catch (IOException e) {
            logger.error("cookie禁用信息写入失败", e);
        }
    }
}
