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
                    List<CookieInfo> valueList = loop();
                    if (null != valueList && valueList.isEmpty()) {
                        for (CookieInfo item : valueList) {
                            if (StringUtils.isNotBlank(item.getCookieName()) && StringUtils.isNotBlank(item.getDomainName()) && StringUtils.isNotBlank(item.getValue())) {
                                cookieRegister.registerCookie(StringUtils.trim(item.getDomainName()), StringUtils.trim(item.getCookieName()), StringUtils.trim(item.getValue()));
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
    public abstract List<CookieInfo> loop();
}
