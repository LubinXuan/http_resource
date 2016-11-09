package com.adtime.http.resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

/**
 * Created by xuanlubin on 2016/11/9.
 */
public class NetworkMonitor {

    private static final Logger logger = LoggerFactory.getLogger(NetworkMonitor.class);

    private static final String MULTI_CAST_IP = "228.7.8.9";

    public static void multicastMonitor(ConnectionAbortUtils.ConnectionAbort connectionAbort) {
        try {
            InetAddress ip = InetAddress.getByName(MULTI_CAST_IP);
            MulticastSocket s = new MulticastSocket(6789);
            s.joinGroup(ip);
            Thread thread = new Thread(() -> {
                DatagramPacket dp = new DatagramPacket(new byte[12], 12);
                while (true) {
                    try {
                        s.receive(dp);
                    } catch (IOException e) {
                        logger.error("消息读取异常!!!", e);
                        continue;
                    }
                    String command = new String(dp.getData(), 0, dp.getLength()).trim();
                    if ("0".equals(command)) {
                        connectionAbort.onAbort();
                    } else {
                        connectionAbort.onStable();
                    }
                    logger.warn("监听到网络变化信息!!!!  {} 操作完成", command);
                }
            });
            thread.setName("NetworkChangeMonitorThread");
            thread.start();
        } catch (IOException e) {
            logger.error("网络状态变更监听广播注册失败", e);
        }
    }

    public static void fileMonitor(ConnectionAbortUtils.ConnectionAbort connectionAbort) {
        String networkMonitorFile = System.getProperty("network.monitor.file", "/tmp");

        if (StringUtils.isNotBlank(networkMonitorFile)) {
            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();
                Path path = Paths.get(networkMonitorFile);
                path.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
                Thread thread = new Thread(() -> {
                    while (true) {
                        WatchKey watchKey = null;
                        try {
                            watchKey = watcher.take();
                        } catch (InterruptedException e) {
                            continue;
                        }

                        for (WatchEvent event : watchKey.pollEvents()) {
                            Object context = event.context();
                            if (context instanceof Path) {
                                String fileName = ((Path) context).toFile().getName();
                                if (StringUtils.equalsIgnoreCase(fileName, "network_reboot_signal")) {
                                    logger.warn("监听到网络变化信息!!!!  {}", event.kind());
                                    if (event.kind().equals(ENTRY_CREATE)) {
                                        connectionAbort.onAbort();
                                    } else if (event.kind().equals(ENTRY_DELETE)) {
                                        connectionAbort.onStable();
                                    }
                                }
                            }
                        }
                        watchKey.reset();
                    }
                });
                thread.setName("NetworkChangeMonitorThread");
                thread.start();
            } catch (IOException e) {
                logger.warn("网络监听文件监听注册失败", e);
            }
        }
    }
}
