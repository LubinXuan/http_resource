package com.adtime.crawl.queue.center;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xuanlubin on 2016/8/24.
 */
public abstract class BaseParallelService<T> implements ParallelService<T> {

    private static final Logger logger = LoggerFactory.getLogger(BaseParallelService.class);

    private static final File STORE_FILE = new File("crawlerParallelConfig.properties");

    protected Map<String, Integer> parallelMapCache = new ConcurrentHashMap<>();

    private TaskCounter taskCounter;

    public void _init() throws IOException {
        if (!STORE_FILE.exists() || STORE_FILE.isDirectory()) {
            return;
        }
        List<String> lines = FileUtils.readLines(STORE_FILE, "UTF-8");
        for (String line : lines) {
            if (StringUtils.startsWith(line, "#")) {
                continue;
            }
            String[] spLine = line.split(":");
            if (spLine.length == 2) {
                try {
                    parallelMapCache.put(spLine[0], Integer.parseInt(spLine[1]));
                } catch (Throwable ignore) {
                }
            }
        }
    }

    @Override
    public void update(String key, int parallel) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        if (parallel < 1) {
            parallel = 10;
        } else if (parallel > 100) {
            parallel = 100;
        }

        parallelMapCache.put(key, parallel);

        taskCounter.updateParallel(key, parallel);

        saveConfig();
    }

    private void saveConfig() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Iterator<Map.Entry<String, Integer>> iterator = parallelMapCache.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
            }
            stringBuilder.append(entry.getKey()).append(":").append(entry.getValue());
        }
        try {
            FileUtils.write(STORE_FILE, stringBuilder.toString(), "utf-8");
        } catch (IOException e) {
            logger.error("并发配置文件写入失败", e);
        }
    }

    public void setTaskCounter(TaskCounter taskCounter) {
        this.taskCounter = taskCounter;
    }
}
