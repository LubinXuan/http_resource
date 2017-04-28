package com.adtime.http.resource.http.nio;

import com.adtime.http.resource.Request;
import com.adtime.http.resource.RequestBuilder;
import com.adtime.http.resource.Result;
import com.adtime.http.resource.url.URLInetAddress;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by xuanlubin on 2017/4/27.
 */
public class NioHttpClient implements Closeable {

    private final ConnectionManager connectionManager;

    private Selector selector;

    private Map<InetAddress, Queue<Request>> queueMap;

    private ByteBuffer buffer = ByteBuffer.allocate(1024 * 8);

    private volatile boolean run = true;

    public NioHttpClient() throws IOException {
        this.selector = Selector.open();
        this.queueMap = new ConcurrentHashMap<>();
        this.connectionManager = new ConnectionManager(5);
        this.startMainSelector();
    }

    private void startMainSelector() {
        new Thread(() -> {
            while (run) {
                try {
                    this.selector.select();
                    Set<SelectionKey> keySet = this.selector.selectedKeys();
                    keySet.forEach(this::processSelectionKey);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        int port = url.getPort();
        boolean https = "https".equalsIgnoreCase(url.getProtocol());
        port = port < 0 ? (https ? 443 : 80) : port;
        SocketChannel channel = this.connectionManager.connect(url.getAuthority(), port);
        RequestWrap requestWrap = new RequestWrap(request, url, responseCallback);
        channel.register(this.selector, SelectionKey.OP_CONNECT, requestWrap);
    }

    private void onConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        channel.configureBlocking(false);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    private void onWriteAble(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        RequestWrap wrap = (RequestWrap) selectionKey.attachment();
        StringBuilder sb = new StringBuilder();
        sb.append(wrap.getRequest().getMethod()).append(" ").append(wrap.getUrl().getPath()).append(" HTTP/1.1\r\n");
        sb.append("Host:").append(wrap.getUrl().getHost()).append("\r\n");
        sb.append("\r\n");
        channel.write(ByteBuffer.wrap(sb.toString().getBytes(Charset.forName("utf-8"))));
        selectionKey.interestOps(SelectionKey.OP_READ);
    }

    private void onReadable(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        RequestWrap wrap = (RequestWrap) selectionKey.attachment();
        int num = -1;
        try {
            while ((num = channel.read(buffer)) > 0) {
                buffer.flip();
                System.out.print(new String(buffer.array(), "UTF-8"));
                buffer.clear();
            }
        } catch (IOException e) {
            selectionKey.cancel();
            this.connectionManager.close(channel);
            wrap.getResponseCallback().failure(wrap.getRequest(), e);
        } finally {
            buffer.clear();
            if (num == 0) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    public void closeAllConnection() {
        //this.connectionManager;
    }

    @Override
    public void close() throws IOException {
        this.run = false;
        this.selector.close();
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        NioHttpClient nioHttpClient = new NioHttpClient();
        Request request = RequestBuilder.buildRequest("http://blog.csdn.net/xxb2008/article/details/27312897");
        nioHttpClient.request(request, new ResponseCallback() {
            @Override
            public void success(Request request, Result result) {

            }

            @Override
            public void failure(Request request, Throwable throwable) {

            }
        });
    }
}
