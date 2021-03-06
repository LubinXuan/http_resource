package com.adtime.http.resource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by xuanlubin on 2016/10/20.
 */
public class ConnectionAbortUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAbortUtils.class);

    //利用DNS服务检测网络是否可用
    private static final String ip = System.getProperty("network.check.ip", "114.114.114.114");

    private static final AtomicBoolean networkDown = new AtomicBoolean(false);

    private static final AtomicBoolean init = new AtomicBoolean(false);

    private static long lastInActive = -1;

    private static final Set<ConnectionAbort> CONNECTION_ABORT_SET = new ConcurrentHashSet<>();

    private static final Set<Predicate<Result>> PREDICATE_SET = new ConcurrentHashSet<>();

    private static final Queue<InputStream> INPUT_STREAMS = new LinkedBlockingQueue<>();

    static void addInputStream(InputStream is) {
        INPUT_STREAMS.add(is);
    }

    static void removeInputStream(InputStream is) {
        INPUT_STREAMS.remove(is);
    }

    public static void register(ConnectionAbort connectionAbort) {
        CONNECTION_ABORT_SET.add(connectionAbort);
    }

    public static void unRegister(ConnectionAbort connectionAbort) {
        CONNECTION_ABORT_SET.remove(connectionAbort);
    }

    public static void addConnectionTest(Predicate<Result> predicate) {
        PREDICATE_SET.add(predicate);
    }

    protected static void init(Consumer<ConnectionAbort> abortConsumer) {

        if (!init.compareAndSet(false, true)) {
            return;
        }
        new Timer("NetworkStatusCheck").schedule(new TimerTask() {
            @Override
            public void run() {
                if (networkDown.get()) {
                    testNetworkStatus();
                    CONNECTION_ABORT.onStable();
                }
            }
        }, 0, 5000);
        abortConsumer.accept(CONNECTION_ABORT);
        PREDICATE_SET.add(result -> StringUtils.contains(result.getMessage(), "Network is unreachable"));
    }

    private static final ConnectionAbort CONNECTION_ABORT = new ConnectionAbort() {
        @Override
        public void onAbort() {
            if (!networkDown.get()) {
                logger.warn("网络中断了");
                networkDown.set(true);
                //更新上次网络故障时间
                lastInActive = System.currentTimeMillis();
                CONNECTION_ABORT_SET.forEach(ConnectionAbort::onAbort);
                while (true) {
                    InputStream is = INPUT_STREAMS.poll();
                    if (null == is) {
                        break;
                    }
                    try {
                        IOUtils.closeQuietly(is);
                    } catch (Exception ignore) {
                        logger.warn("关闭流发生异常", ignore);
                    }
                }
            }
        }

        @Override
        public void onStable() {
            if (networkDown.get()) {
                logger.warn("网络恢复了");
                networkDown.set(false);
                synchronized (networkDown) {
                    networkDown.notifyAll();
                }
                CONNECTION_ABORT_SET.forEach(ConnectionAbort::onStable);
            }
        }
    };

    private static void testNetworkStatus() {
        while (true) {
            Socket socket = new Socket();
            try {
                socket.setSoTimeout(1000);
                socket.connect(new InetSocketAddress(ip, 53));
                socket.close();
                logger.warn("Network stable");
                break;
            } catch (IOException e) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


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

        boolean isNetworkOut = false;

        for (Predicate<Result> predicate : PREDICATE_SET) {
            isNetworkOut = predicate.test(result);
            if (isNetworkOut) {
                break;
            }
        }

        if (isNetworkOut) {
            networkDown.set(true);
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
        if (networkDown.get()) {
            logger.warn("Network is unreachable wait it stable");
            synchronized (networkDown) {
                if (networkDown.get()) {
                    try {
                        networkDown.wait();
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
        return readStartTime < lastInActive || networkDown.get();
    }

    /**
     * 网络监听
     */
    public interface ConnectionAbort {

        /**
         * 通知网络断开
         */
        void onAbort();

        /**
         * 通知网络恢复
         */
        void onStable();
    }

}
