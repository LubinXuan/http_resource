package com.adtime.jms;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by xuanlubin on 2016/10/27.
 */
public class JMSRspDataSender implements Closeable {

    private final BlockingQueue<File> fileQueue;

    private static final Logger logger = LoggerFactory.getLogger(JMSRspDataSender.class);

    private File dir;

    private final BlockingQueue<RspData> rspBlockingQueue = new LinkedBlockingQueue<>();

    private boolean shutdown = false;

    private boolean writeFile = false;

    public JMSRspDataSender(String folder, JMSDataHandler jmsDataHandler) {
        dir = new File(folder);
        if (!dir.exists() || !dir.isDirectory()) {
            try {
                FileUtils.forceMkdir(dir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        startFileWriteThread();

        boolean jmsUpload = Boolean.parseBoolean(System.getProperty("jms.upload", "true"));

        if (null == jmsDataHandler || !jmsUpload) {
            logger.warn("数据只写出文件,未启动发送线程!! 文件输出目录:{}", dir.getAbsolutePath());
            fileQueue = null;
        } else {
            fileQueue = new LinkedBlockingQueue<>();
            File[] files = dir.listFiles(file -> file.isFile() && file.getName().endsWith(".txt"));
            if (null != files && files.length > 0) {
                Collections.addAll(fileQueue, files);
            }
            startJmsSendThread(jmsDataHandler, fileQueue);
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
            int count = 0;
            StringBuilder builder = new StringBuilder();
            while (true) {
                RspData taskRsp = rspBlockingQueue.poll();
                if (null != taskRsp) {
                    builder.append(taskRsp.destination).append(":").append(taskRsp.data).append("\r\n");
                    count++;
                    if (count > 1000) {
                        writeToFile(builder);
                        count = 0;
                    }
                } else {
                    if (count != 0) {
                        writeToFile(builder);
                        count = 0;
                    }

                    if (shutdown) {
                        break;
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

    private void startJmsSendThread(JMSDataHandler jmsDataHandler, final BlockingQueue<File> _fileQueue) {
        Runnable jmsSendRunnable = () -> {

            logger.info("JMS发送线程启动");

            while (!shutdown) {
                List<String> stringList = null;
                File file = null;
                try {
                    file = _fileQueue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                logger.info("开始发送数据:{}", file.getAbsolutePath());
                try {
                    stringList = FileUtils.readLines(file, "utf-8");
                } catch (FileNotFoundException e) {
                    continue;
                } catch (IOException e) {
                    _fileQueue.offer(file);
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
                            if (e instanceof FileNotFoundException) {
                                break;
                            } else if (e instanceof IOException) {
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
                    } catch (FileNotFoundException ignore) {

                    } catch (IOException e) {
                        logger.error("文件删除失败", e);
                    }
                }
            }

            logger.info("JMS发送线程退出!!!");
        };
        Thread jmsDataSendThread = new Thread(jmsSendRunnable);
        jmsDataSendThread.setName("JmsDataSendThread");
        jmsDataSendThread.start();
    }

    private void writeToFile(StringBuilder builder) {
        writeFile = true;
        String fileName = Long.toString(System.currentTimeMillis());
        File tmp = new File(dir, fileName + ".tmp");
        while (true) {
            try {
                FileUtils.write(tmp, builder.toString(), Charset.forName("utf-8"));
                File file = new File(dir, fileName + ".txt");
                FileUtils.moveFile(tmp, file);
                if (null != fileQueue) {
                    fileQueue.offer(file);
                }
                break;
            } catch (IOException e) {
                try {
                    logger.warn("文件写出异常::::等待5s 重试  {}", e);
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        builder.setLength(0);
        writeFile = false;
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

    @Override
    public void close() throws IOException {
        this.shutdown = true;
        while (pending() > 0 || writeFile) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignore) {

            }
        }
    }
}
