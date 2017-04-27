package com.adtime.http.resource.http.nio;

import com.adtime.http.resource.Request;
import com.adtime.http.resource.url.URLInetAddress;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2017/4/27.
 */
public class NioHttpClient implements Closeable {

    private final ConnectionManager connectionManager;

    private Selector selector;

    private Map<InetAddress, Queue<Request>> queueMap;

    private ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);

    public NioHttpClient() throws IOException {
        this.selector = Selector.open();
        this.queueMap = new ConcurrentHashMap<>();
        this.connectionManager = new ConnectionManager(5);
    }

    private void startMainSelector() {
        try {
            int keys = this.selector.select(500L);
            if (keys > 0) {
                Set<SelectionKey> keySet = this.selector.selectedKeys();
                keySet.forEach(this::processSelectionKey);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void processSelectionKey(SelectionKey selectionKey) {
        try {
            if (selectionKey.isConnectable()) {
                onConnect(selectionKey);
            } else if (selectionKey.isReadable()) {
                onReadable(selectionKey);
            } else if (selectionKey.isWritable()) {
                onWriteAble(selectionKey);
            }
        } catch (IOException e) {
            //todo 返回异常数据
        }
    }

    private void request(Request request, ResponseCallback responseCallback) throws IOException, InterruptedException {
        URL url = URLInetAddress.create(request.requestUrl());
        String host = url.getHost();
        int port = url.getPort();
        SocketChannel channel = this.connectionManager.connect(host, port);
        SelectionKey selectionKey = channel.register(this.selector, SelectionKey.OP_CONNECT);
        selectionKey.attach(new RequestWrap(request, responseCallback));
    }

    public void onConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        channel.configureBlocking(false);
        channel.register(this.selector, SelectionKey.OP_WRITE);
    }

    public void onWriteAble(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        RequestWrap wrap = (RequestWrap) selectionKey.attachment();

    }

    public void onReadable(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        RequestWrap wrap = (RequestWrap) selectionKey.attachment();
        while (channel.read(buffer) > 0) {
            buffer.flip();
            System.out.println("Receive from server:" + new String(buffer.array(), "UTF-8"));
            buffer.clear();
        }
    }

    public void closeAllConnection() {
        //this.connectionManager;
    }

    @Override
    public void close() throws IOException {
        this.selector.close();
    }
}
