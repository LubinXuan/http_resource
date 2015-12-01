package com.adtime.http.resource.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adtime.http.resource.url.CookieRegister;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2015/11/30.
 */
public abstract class CookieUpdater {

    private static final Logger logger = LoggerFactory.getLogger(CookieUpdater.class);

    public CookieUpdater(CookieRegister cookieRegister) {
        new Timer("cookie-updater").schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<String[]> valueList = loop();
                    if (null != valueList && valueList.isEmpty()) {
                        for (String[] item : valueList) {
                            if (item.length >= 3 && StringUtils.isNotBlank(item[0]) && StringUtils.isNotBlank(item[1]) && StringUtils.isNotBlank(item[2])) {
                                cookieRegister.registerCookie(StringUtils.trim(item[0]), StringUtils.trim(item[1]), StringUtils.trim(item[2]));
                            }
                        }
                    }
                } catch (Throwable e) {
                    logger.error("cookie 信息更新失败!!!!");
                }
            }
        }, 10000, 10000);
    }

    /**
     * 返回 cookie 列表
     * item a1:domainName a2:cookieName a3:cookieValue
     *
     * @return
     */
    public abstract List<String[]> loop();
}
