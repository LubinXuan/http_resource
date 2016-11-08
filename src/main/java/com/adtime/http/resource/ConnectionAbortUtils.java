package com.adtime.http.resource;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

/**
 * Created by xuanlubin on 2016/10/20.
 */
public class ConnectionAbortUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionAbortUtils.class);

    //利用DNS服务检测网络是否可用
    private static final String ip = System.getProperty("network.check.ip", "114.114.114.114");

    private static final String MULTI_CAST_IP = "228.7.8.9";

    private static final AtomicBoolean networkDown = new AtomicBoolean(false);

    private static final AtomicBoolean init = new AtomicBoolean(false);

    private static long lastInActive = -1;

    private static final Set<ConnectionAbort> CONNECTION_ABORT_SET = new ConcurrentHashSet<>();

    public static void register(ConnectionAbort connectionAbort) {
        CONNECTION_ABORT_SET.add(connectionAbort);
    }

    public static void unRegister(ConnectionAbort connectionAbort) {
        CONNECTION_ABORT_SET.remove(connectionAbort);
    }

    protected static void init() {

        if (!init.compareAndSet(false, true)) {
            return;
        }

        try {
            InetAddress ip = InetAddress.getByName(MULTI_CAST_IP);
            MulticastSocket s = new MulticastSocket(6789);
            s.joinGroup(ip);
            Thread thread = new Thread(() -> {
                byte[] arb = new byte[1];
                while (true) {
                    DatagramPacket datagramPacket = new DatagramPacket(arb, arb.length);
                    try {
                        s.receive(datagramPacket);
                    } catch (IOException e) {
                        logger.error("消息读取异常!!!", e);
                        continue;
                    }
                    String command = new String(arb).trim();
                    arb = new byte[1];
                    logger.warn("监听到网络变化信息!!!!  {}", command);
                    if ("0".equals(command)) {
                        networkDown.set(true);
                        //更新上次网络故障时间
                        lastInActive = System.currentTimeMillis();
                        CONNECTION_ABORT_SET.forEach(ConnectionAbort::onAbort);
                    } else {
                        networkDown.set(false);
                        synchronized (networkDown) {
                            networkDown.notifyAll();
                        }
                        CONNECTION_ABORT_SET.forEach(ConnectionAbort::onStable);
                    }
                }
            });
            thread.setName("NetworkChangeMonitorThread");
            thread.start();
        } catch (IOException e) {
            logger.error("网络状态变更监听广播注册失败", e);
        }
    }

    private static final Runnable checkNetworkRunnable = () -> {

        if (!networkDown.get()) {
            return;
        }

        logger.warn("Start Network Check Thread");

        //更新上次网络故障时间
        lastInActive = System.currentTimeMillis();

        testNetworkStatus();

        networkDown.set(false);

        synchronized (networkDown) {
            networkDown.notifyAll();
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

        boolean isNetworkOut = StringUtils.contains(result.getMessage(), "Network is unreachable");

        if (isNetworkOut) {
            if (networkDown.compareAndSet(false, true)) {
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
