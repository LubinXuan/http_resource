package com.adtime.http.resource.util;

import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebConst;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by xuanlubin on 2016/10/20.
 */
public class ConnectionAbortUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAbortUtils.class);

    public static boolean isNetworkOut(Result result) {
        boolean isNetworkOut = result.getStatus() == WebConst.HTTP_ERROR && StringUtils.contains(result.getMessage(), "Network is unreachable");
        if (isNetworkOut) {
            logger.warn("Network is unreachable wait 5 second");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return isNetworkOut;
    }
}
