package com.adtime.crawl.queue.server;

import com.adtime.http.resource.HttpIns;
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
import java.util.Base64;
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

    private String basicAuthInfo = null;

    public HttpQueueOperator(String fileStoreDir, String server) {
        this(fileStoreDir, server, null);
    }

    public HttpQueueOperator(String fileStoreDir, String server, String auth) {
        this.fileStoreDir = new File(fileStoreDir);
        if (!this.fileStoreDir.exists() || !this.fileStoreDir.isDirectory()) {
            if (!this.fileStoreDir.mkdir()) {
                throw new RuntimeException("本地存储目录创建失败:" + this.fileStoreDir.getAbsolutePath());
            }
        }

        if (StringUtils.isBlank(server)) {
            client = null;
            serverHost = null;
            logger.warn("没有指定服务器IP地址");
        } else {
            serverHost = HttpHost.create("http://" + server);
            client = HttpIns.global();
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
                            logger.error("Http请求文件读取异常", e);
                            fileBlockingQueue.offer(file);
                            continue;
                        }

                        if (content.isEmpty()) {
                            FileUtils.deleteQuietly(file);
                            continue;
                        }

                        HttpRsp rsp;

                        if (content.size() > 1) {
                            rsp = _send(content.get(0), content.get(1));
                        } else {
                            rsp = _send(content.get(0), null);
                        }

                        if (rsp.status != 200) {
                            fileBlockingQueue.offer(file);
                            try {
                                TimeUnit.SECONDS.sleep(10);
                            } catch (InterruptedException ignore) {
                            }
                        } else {
                            FileUtils.deleteQuietly(file);
                        }
                    } catch (InterruptedException e) {
                        logger.error("Http线程异常!!", e);
                    }
                }
            });
            taskSendThread.setName("HttpTaskSendThread-" + fileStoreDir);
            taskSendThread.start();
        }
        if (StringUtils.isNotBlank(auth)) {
            basicAuthInfo = Base64.getEncoder().encodeToString(auth.getBytes());
        }
    }

    public HttpRsp send(String request, String jsonData) {
        return send(request, jsonData, false);
    }

    public HttpRsp send(String request, String jsonData, boolean saveOnFail) {
        HttpRsp httpRsp = _send(request, jsonData);
        boolean success = 200 == httpRsp.status;
        if (!success && saveOnFail) {
            try {
                File out = new File(fileStoreDir, System.currentTimeMillis() + "-" + id.incrementAndGet() + ".txt");
                FileUtils.write(out, request + "\r\n" + jsonData, "utf-8");
                fileBlockingQueue.offer(out);
            } catch (IOException e) {
                logger.error("Http请求文件写出失败", e);
            }
        }
        return httpRsp;
    }

    public HttpRsp _send(String request, String jsonData) {

        HttpRsp httpRsp = new HttpRsp();

        if (null == serverHost || null == client) {
            httpRsp.status = -1;
            return httpRsp;
        }

        if (StringUtils.isBlank(request)) {
            return httpRsp;
        }

        HttpRequestBase httpRequest;
        if (StringUtils.isNotBlank(jsonData)) {
            httpRequest = new HttpPost(request);
            ((HttpPost) httpRequest).setEntity(new StringEntity(jsonData, Charset.forName("utf-8")));
        } else {
            httpRequest = new HttpGet(request);
        }


        try {

            if (StringUtils.isNotBlank(basicAuthInfo)) {
                httpRequest.addHeader("Authorization", "Basic " + basicAuthInfo);
            }

            HttpResponse response = client.execute(serverHost, httpRequest);
            httpRsp.status = response.getStatusLine().getStatusCode();
            if (200 == httpRsp.status) {
                httpRsp.content = EntityUtils.toString(response.getEntity(), "utf-8");
                if (logger.isDebugEnabled()) {
                    logger.debug("Http请求成功 {} {}", request, jsonData);
                } else {
                    logger.info("Http请求成功 {} {}", request, StringUtils.length(jsonData));
                }
            }
            EntityUtils.consumeQuietly(response.getEntity());
        } catch (Exception e) {
            logger.error("Http执行异常:" + request, e);
            httpRsp.status = -1;
        } finally {
            httpRequest.releaseConnection();
        }
        return httpRsp;
    }

    public class HttpRsp {
        private int status = 200;
        private String content = null;

        public int getStatus() {
            return status;
        }

        public String getContent() {
            return content;
        }
    }

}
