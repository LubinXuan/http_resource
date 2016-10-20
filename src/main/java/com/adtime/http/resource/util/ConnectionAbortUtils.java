package com.adtime.http.resource.util;

import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebConst;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xuanlubin on 2016/10/20.
 */
public class ConnectionAbortUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAbortUtils.class);

    private static final String ip = System.getProperty("network.check.ip", "114.114.114.114");

    private static final AtomicBoolean checkNetwork = new AtomicBoolean(false);

    private static final Runnable checkNetworkRunnable = () -> {

        logger.warn("Start Network Check Thread");

        while (true) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(ip, 53));
                socket.close();

                checkNetwork.set(false);

                synchronized (checkNetwork) {
                    checkNetwork.notifyAll();

                }
                logger.warn("Network stable");

                break;
            } catch (IOException e) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    };

    public static boolean isNetworkOut(Result result) {
        boolean isNetworkOut = result.getStatus() == WebConst.HTTP_ERROR && StringUtils.contains(result.getMessage(), "Network is unreachable");
        if (isNetworkOut) {
            if (checkNetwork.compareAndSet(false, true)) {
                new Thread(checkNetworkRunnable).start();
            }

            isNetworkOut();

        }
        return isNetworkOut;
    }


    public static void isNetworkOut() {
        if (checkNetwork.get()) {
            logger.warn("Network is unreachable wait it stable");
            synchronized (checkNetwork) {
                if (checkNetwork.get()) {
                    try {
                        checkNetwork.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
