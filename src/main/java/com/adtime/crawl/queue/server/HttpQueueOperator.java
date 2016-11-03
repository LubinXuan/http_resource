package com.adtime.crawl.queue.server;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by xuanlubin on 2016/11/1.
 */
public class HttpQueueOperator {

    private static final Logger logger = LoggerFactory.getLogger(HttpQueueOperator.class);

    private final HttpClient client;

    private final HttpHost serverHost;

    private final File fileStoreDir;

    private final BlockingQueue<File> fileBlockingQueue = new LinkedBlockingQueue<>();

    private static final AtomicLong id = new AtomicLong(0);

    public HttpQueueOperator(String fileStoreDir, String server) {
        this.fileStoreDir = new File(fileStoreDir);
        if (!this.fileStoreDir.exists() || this.fileStoreDir.isDirectory()) {
            if (!this.fileStoreDir.mkdir()) {
                throw new RuntimeException("本地存储目录创建失败:" + this.fileStoreDir.getAbsolutePath());
            }
        }

        serverHost = HttpHost.create("http://" + server);

        RequestConfig config = RequestConfig.custom().setConnectTimeout(30000).setSocketTimeout(30000).build();
        client = HttpClients.custom().setDefaultRequestConfig(config).build();

        File[] files = this.fileStoreDir.listFiles(file -> file.isFile() && file.getName().endsWith(".txt"));

        if (null != files) {
            Collections.addAll(fileBlockingQueue, files);
        }

        Thread taskSendThread = new Thread(() -> {
            while (true) {
                try {
                    File file = fileBlockingQueue.take();
                    List<String> content;
                    try {
                        content = FileUtils.readLines(file, "utf-8");
                    } catch (IOException e) {
                        logger.error("资源释放请求文件读取异常", e);
                        fileBlockingQueue.offer(file);
                        continue;
                    }

                    if (content.isEmpty()) {
                        FileUtils.deleteQuietly(file);
                        continue;
                    }

                    boolean success;

                    if (content.size() > 1) {
                        success = (boolean) _send(content.get(0), content.get(1))[0];
                    } else {
                        success = (boolean) _send(content.get(0), null)[0];
                    }

                    if (!success) {
                        fileBlockingQueue.offer(file);
                        try {
                            TimeUnit.SECONDS.sleep(10);
                        } catch (InterruptedException ignore) {
                        }
                    } else {
                        FileUtils.deleteQuietly(file);
                    }
                } catch (InterruptedException e) {
                    logger.error("资源释放线程异常!!", e);
                }
            }
        });
        taskSendThread.setName("HttpTaskSendThread");
        taskSendThread.start();

    }

    protected String send(String request, String jsonData, boolean saveOnFail) {
        Object ret[] = _send(request, jsonData);
        boolean success = (boolean) ret[0];
        if (!success && saveOnFail) {
            try {
                File out = new File(fileStoreDir, System.currentTimeMillis() + "-" + id.incrementAndGet() + ".txt");
                FileUtils.write(out, request + "\r\n" + jsonData, "utf-8");
                fileBlockingQueue.offer(out);
            } catch (IOException e) {
                logger.error("Http请求文件写出失败", e);
            }
        }
        return (String) ret[1];
    }

    private Object[] _send(String request, String jsonData) {

        if (StringUtils.isBlank(request)) {
            return new Object[]{true, null};
        }

        HttpRequestBase httpRequest;
        if (StringUtils.isNotBlank(jsonData)) {
            httpRequest = new HttpPost(request);
            ((HttpPost) httpRequest).setEntity(new StringEntity(jsonData, Charset.forName("utf-8")));
        } else {
            httpRequest = new HttpGet(request);
        }

        Object[] ret = new Object[]{true, null};

        try {
            HttpResponse response = client.execute(serverHost, httpRequest);
            ret[0] = 200 == response.getStatusLine().getStatusCode();
            ret[1] = EntityUtils.toString(response.getEntity());
            EntityUtils.consumeQuietly(response.getEntity());
            return ret;
        } catch (Exception e) {
            logger.error("Http执行异常:" + request, e);
            ret[0] = false;
        } finally {
            httpRequest.releaseConnection();
        }
        return ret;
    }

}
