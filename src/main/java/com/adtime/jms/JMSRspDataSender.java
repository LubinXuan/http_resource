package com.adtime.jms;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by xuanlubin on 2016/10/27.
 */
public class JMSRspDataSender {

    private final BlockingQueue<File> fileQueue = new LinkedBlockingQueue<>();

    private static final Logger logger = LoggerFactory.getLogger(JMSRspDataSender.class);

    private File dir;

    private final BlockingQueue<RspData> rspBlockingQueue = new LinkedBlockingQueue<>();

    public JMSRspDataSender(String folder, JMSDataHandler jmsDataHandler) {
        dir = new File(folder);
        if (!dir.exists() || !dir.isDirectory()) {
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        File[] files = dir.listFiles(file -> file.isFile() && file.getName().endsWith(".txt"));

        if (null != files && files.length > 0) {
            Collections.addAll(fileQueue, files);
        }

        startFileWriteThread();
        if (null == jmsDataHandler) {
            logger.warn("数据只写出文件,未启动发送线程!! 文件输出目录:{}", dir.getAbsolutePath());
        } else {
            startJmsSendThread(jmsDataHandler);
        }
    }

    public JMSRspDataSender(String folder) {
        this(folder, null);
    }

    public void send(String data, String destination) {
        rspBlockingQueue.offer(new RspData(destination, data));
    }

    public int pending() {
        return rspBlockingQueue.size();
    }

    private void startFileWriteThread() {
        Runnable fileWriteRunnable = () -> {
            logger.info("爬虫结果数据文件写出线程启动");
            FileOutputStream fos = null;
            String fileName = null;
            int count = 0;
            while (true) {
                RspData taskRsp = rspBlockingQueue.poll();
                if (null != taskRsp) {
                    if (null == fos) {
                        fileName = Long.toString(System.currentTimeMillis());
                        fos = createFos(dir, fileName);
                    }

                    try {
                        IOUtils.write(taskRsp.getDestination() + ":" + taskRsp.getData() + "\r\n", fos, "utf-8");
                    } catch (Throwable e) {
                        logger.warn("数据写入文件异常!!! {}", e);
                        rspBlockingQueue.offer(taskRsp);
                    }

                    count++;

                    if (count > 1000) {
                        renameFile(fos, dir, fileName);
                        fos = null;
                        count = 0;
                    }
                } else {
                    if (count != 0) {
                        renameFile(fos, dir, fileName);
                        fos = null;
                        count = 0;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException ignore) {

                    }
                }
            }
        };

        Thread fileWriteThread = new Thread(fileWriteRunnable);
        fileWriteThread.setName("CrawlDataFileWriteThread");
        fileWriteThread.start();
    }

    private void startJmsSendThread(JMSDataHandler jmsDataHandler) {
        Runnable jmsSendRunnable = () -> {

            logger.info("JMS发送线程启动");

            while (true) {
                List<String> stringList = null;
                File file = null;
                try {
                    file = fileQueue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                logger.info("开始发送数据:{}", file.getAbsolutePath());
                try {
                    stringList = FileUtils.readLines(file, "utf-8");
                } catch (IOException e) {
                    fileQueue.offer(file);
                    continue;
                }

                if (null != stringList && !stringList.isEmpty()) {

                    Map<String, List<String>> dataMap = new HashMap<>();
                    for (String line : stringList) {
                        String[] q_msg = StringUtils.split(line, ":", 2);
                        dataMap.computeIfAbsent(q_msg[0], s -> new ArrayList<>()).add(q_msg[1]);
                    }

                    long start = System.currentTimeMillis();

                    while (true) {
                        try {
                            if (jmsDataHandler.handle(dataMap)) {
                                FileUtils.forceDelete(file);
                            }
                            break;
                        } catch (Exception e) {
                            if (e instanceof IOException) {
                                logger.error("文件删除失败", e);
                            } else {
                                try {
                                    TimeUnit.SECONDS.sleep(10);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }

                    logger.info("数据上传完成 数据量:{} 上传耗时:{}", stringList.size(), System.currentTimeMillis() - start);
                } else {
                    try {
                        FileUtils.forceDelete(file);
                    } catch (IOException e) {
                        logger.error("文件删除失败", e);
                    }
                }
            }
        };
        Thread jmsDataSendThread = new Thread(jmsSendRunnable);
        jmsDataSendThread.setName("JmsDataSendThread");
        jmsDataSendThread.start();
    }

    private FileOutputStream createFos(File dir, String fileName) {
        try {
            return new FileOutputStream(new File(dir, fileName + ".txt"));
        } catch (Exception e) {
            logger.error("文件流获取失败", e);
            return null;
        }
    }

    private void renameFile(FileOutputStream fos, File dir, String fileName) {
        IOUtils.closeQuietly(fos);
        fileQueue.offer(new File(dir, fileName + ".txt"));
    }

    private static class RspData {
        private String destination;

        private String data;

        public RspData(String destination, String data) {
            this.destination = destination;
            this.data = data;
        }

        public String getDestination() {
            return destination;
        }

        public String getData() {
            return data;
        }
    }

    public interface JMSDataHandler {
        boolean handle(Map<String, List<String>> dataMapList);
    }
}
