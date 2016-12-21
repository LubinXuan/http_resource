package com.adtime.crawl.queue.center;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by xuanlubin on 2016/8/10.
 */
public class DefaultQueuePersistService<P, T extends Identity<P>> implements QueuePersistService<P, T> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultQueuePersistService.class);

    private AtomicInteger counter = new AtomicInteger(0);

    private ConcurrentHashMap<P, T> concurrentHashMap = new ConcurrentHashMap<>();

    private File storeFile;

    private final String storeName;

    public DefaultQueuePersistService(String storeName, Function<String, T> tFunction) {
        this.storeName = storeName;
        storeFile = new File(storeName);

        try {
            List<String> saveDataList = FileUtils.readLines(storeFile, Charset.defaultCharset());
            for (String task : saveDataList) {
                try {
                    T seedTask = tFunction.apply(task);
                    concurrentHashMap.put(seedTask.getId(), seedTask);
                } catch (Throwable e) {
                    logger.error("任务反序列化异常!!~~~", e);
                }
            }
        } catch (FileNotFoundException e) {
            logger.debug("没有发现本地任务存储文件");
        } catch (Exception e) {
            logger.error("无法从本地文件恢复任务！！！", e);
        }
        new Timer("PersistLocalFile").schedule(new TimerTask() {
            @Override
            public void run() {
                saveFile();
            }
        }, 10000, 10000);
    }

    protected void saveFile() {
        int update = counter.getAndSet(0);
        if (update > 0) {
            try {
                File tmp = new File(this.storeName + "." + System.currentTimeMillis());
                List<T> seedTaskList = new ArrayList<>(concurrentHashMap.values());
                StringBuilder builder = new StringBuilder();
                for (T seedTask : seedTaskList) {
                    builder.append(JSON.toJSONString(seedTask)).append("\n");
                }
                FileUtils.write(tmp, builder.toString(), Charset.defaultCharset());
                logger.warn("任务序列化临时文件生成完毕");
                FileUtils.deleteQuietly(storeFile);
                FileUtils.moveFile(tmp, storeFile);
            } catch (IOException e) {
                logger.error("任务文件持久化失败", e);
            }
        }
    }


    //QueuePersistService
    @Override
    public void save(T task) {
        counter.incrementAndGet();
        concurrentHashMap.put(task.getId(), task);
    }

    @Override
    public List<T> listQueueTask() {
        return new ArrayList<>(concurrentHashMap.values());
    }


    @Override
    public void deleteTask(P id) {
        if (null != concurrentHashMap.remove(id)) {
            counter.incrementAndGet();
        }
    }

    @Override
    public void close() {
        saveFile();
    }
}
