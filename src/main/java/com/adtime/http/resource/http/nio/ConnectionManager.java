package com.adtime.http.resource.http.nio;

import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xuanlubin on 2017/4/27.
 */
public class ConnectionManager {

    private int maxConnectionPerHost = 5;

    private Map<String, ImmutableTriple<Integer, AtomicInteger, BlockingQueue<SocketChannel>>> channelMap = new ConcurrentHashMap<>();

    public ConnectionManager(int maxConnectionPerHost) {
        this.maxConnectionPerHost = maxConnectionPerHost < 1 ? 5 : maxConnectionPerHost;
    }

    public SocketChannel connect(String ip, int port) throws IOException, InterruptedException {
        ImmutableTriple<Integer, AtomicInteger, BlockingQueue<SocketChannel>> triple = this.channelMap.computeIfAbsent(ip + ":" + port, s -> new ImmutableTriple<>(maxConnectionPerHost, new AtomicInteger(0), new LinkedBlockingQueue<>()));
        SocketChannel channel = triple.getRight().poll();
        if (null == channel || channel.isOpen()) {
            synchronized (triple) {
                if (triple.getLeft() > triple.getMiddle().get()) {
                    channel = SocketChannel.open();
                    channel.configureBlocking(false);
                    channel.connect(new InetSocketAddress(ip, port));
                    return channel;
                } else {
                    return triple.getRight().take();
                }
            }
        } else {
            return channel;
        }
    }

    public void close(SocketChannel channel) throws IOException {
        channel.close();
    }
}
