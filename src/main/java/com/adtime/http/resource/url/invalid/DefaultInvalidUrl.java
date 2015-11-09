package com.adtime.http.resource.url.invalid;

import com.adtime.http.resource.common.ConfigReader;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class DefaultInvalidUrl implements InvalidUrl {

    private static final Logger logger = LoggerFactory.getLogger(DefaultInvalidUrl.class);

    public final static String DEFAULT_CONFIG = "http/config/invalidUrl.json";

    private InvalidDef invalidDef;

    public DefaultInvalidUrl(String invalidUrlSource) {
        try {
            String data = ConfigReader.readNOE(invalidUrlSource);
            invalidDef = JSON.parseObject(data, InvalidDef.class);
        } catch (Exception e) {
            logger.error("非法url初始化出错", e);
        }

    }

    public DefaultInvalidUrl() {
        this(DEFAULT_CONFIG);
    }

    public boolean valid(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        try {
            URL _url = new URL(url);
            if (!_url.getProtocol().equals("http") && !_url.getProtocol().equals("https")) {
                return false;
            }
            for (String host : invalidDef.getHost()) {
                if (_url.getHost().contains(host)) {
                    return false;
                }
            }
            int lastDot = _url.getPath().lastIndexOf(".");
            if (lastDot > _url.getPath().lastIndexOf("/")) {
                String suffix = _url.getPath().substring(lastDot + 1).toLowerCase();
                if (invalidDef.getSuffix().contains(suffix)) {
                    return false;
                }
            }
            for (String part : invalidDef.getUrlPart()) {
                if (url.contains(part)) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
