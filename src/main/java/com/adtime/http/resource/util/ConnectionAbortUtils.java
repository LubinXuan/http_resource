package com.adtime.http.resource.util;

import com.adtime.http.resource.Request;
import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebConst;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by xuanlubin on 2016/10/20.
 */
public class ConnectionAbortUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAbortUtils.class);

    //利用DNS服务检测网络是否可用
    private static final String ip = System.getProperty("network.check.ip", "114.114.114.114");

    private static final AtomicBoolean checkNetwork = new AtomicBoolean(false);

    private static WatchKey watchKey = null;

    private static long lastInActive = -1;

    static {

        String networkMonitorFile = System.getProperty("network.monitor.file", "C:\\adsl");

        if (StringUtils.isNotBlank(networkMonitorFile)) {
            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();
                Path path = Paths.get(networkMonitorFile);
                watchKey = path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            } catch (IOException e) {
                logger.warn("网络监听文件监听注册失败", e);
            }
        }
    }

    private static final Runnable checkNetworkRunnable = () -> {

        logger.warn("Start Network Check Thread");


        //更新上次网络故障时间
        lastInActive = System.currentTimeMillis();

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

    /**
     * 根据请求返回的状态以及错误信息判断是否网络中断导致本次请求异常
     *
     * @param result  请求结果
     * @param request 请求信息
     * @return
     */
    public static boolean isNetworkOut(Result result, Request request) {

        if (result.getStatus() != WebConst.HTTP_ERROR) {
            return false;
        }

        boolean isNetworkOut = StringUtils.contains(result.getMessage(), "Network is unreachable");

        if (!isNetworkOut && null != watchKey) {
            List<WatchEvent<?>> watchEventList = watchKey.pollEvents();
            for (WatchEvent event : watchEventList) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    isNetworkOut = true;
                    break;
                }
            }
        }

        if (isNetworkOut) {
            if (checkNetwork.compareAndSet(false, true)) {
                new Thread(checkNetworkRunnable).start();
            }
            checkNetworkStatus();
            return true;
        } else if (checkNetworkStatus()) {
            return true;
        }

        //判断请求时间与上次网络故障时间
        if (request.getHttpExecStartTime() < lastInActive) {
            logger.debug("request start before last network inactive time");
            return true;
        }

        return false;
    }


    /**
     * 判定网络状态，如果网络不可用，线程进入等待状态
     */
    public static boolean checkNetworkStatus() {
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
            return true;
        } else {
            return false;
        }
    }


    /**
     * 更具数据开始读取的时间判断是否网络中断
     *
     * @param readStartTime 数据读取时间
     * @return
     */
    public static boolean isNetworkOut(long readStartTime) {
        return readStartTime < lastInActive || checkNetwork.get();
    }
}
