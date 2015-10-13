package com.adtime.http.resource;

import com.adtime.http.resource.common.ConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by Lubin.Xuan on 2015/5/25.
 * ie.
 */
@SuppressWarnings("all")
public class WebConst {

    private static final Logger logger = LoggerFactory.getLogger(WebConst.class);

    public static int SELF_CODE_START = 35000;
    public static int SELF_CODE_END = 36000;

    public static int DOWNLOAD_STREAM = 35999;
    public static int HTTP_ERROR = 18000;
    public static int LOCAL_HOST_ERROR = 35410;
    public static int LOCAL_NOT_HTML = 35405;
    public static int TITLE_NOT_FONUD = 35404;
    public static int LOCAL_NOT_ACCEPTABLE = 35406;
    public static int HTTP_NO_CONTENT = 35204;
    public static int LOCAL_DEAL_ERROR = 35500;
    public static int LOCAL_INDEX_DENY = 35407;
    public static int NULL_URL = -999;


    private static ArrayList<String> UA = new ArrayList<>();
    private static ArrayList<String> MO_UA = new ArrayList<>();

    private static int total_ua_count = 0;
    private static int total_mo_ua_count = 0;

    private static String DEFAULT_UA, DEFAULT_MOBI_UA;

    static {
        UA.addAll(readUa("http/config/ua.conf"));
        MO_UA.addAll(readUa("http/config/ua.mobi.conf"));
        total_ua_count = UA.size();
        total_mo_ua_count = MO_UA.size();
        DEFAULT_UA = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.143 Safari/537.36";
        DEFAULT_MOBI_UA = "Mozilla/5.0 (Linux; Android 4.4.4; en-us; Nexus 4 Build/JOP40D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2307.2 Mobile Safari/537.36";
    }


    private static Set<String> readUa(String path) {
        Set<String> uaSet = new HashSet<>();
        try {
            String[] uaArr = ConfigReader.readNOE(path).split("\n");
            for (String ua : uaArr) {
                if (ua.trim().length() > 0) {
                    uaSet.add(ua);
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return uaSet;
    }

    private static final Random UA_RANDOM = new Random();

    public static String randomUA(boolean mobile) {
        if (mobile) {
            return randomMobileUA();
        } else {
            return randomUA();
        }
    }

    public static String randomUA() {
        if (total_ua_count > 0) {
            return UA.get(UA_RANDOM.nextInt(total_ua_count));
        } else {
            return DEFAULT_UA;
        }
    }

    public static String randomMobileUA() {
        if (total_mo_ua_count > 0) {
            return MO_UA.get(UA_RANDOM.nextInt(total_mo_ua_count));
        } else {
            return DEFAULT_MOBI_UA;
        }
    }
}
