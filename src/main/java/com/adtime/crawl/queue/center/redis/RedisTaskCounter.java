package com.adtime.crawl.queue.center.redis;

import com.adtime.bullbat.queue.center.Identity;
import com.adtime.bullbat.queue.center.TaskCounter;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Lubin.Xuan on 2015/6/17.
 * ie.
 */
public class RedisTaskCounter<P, T extends Identity<P>> extends TaskCounter<P,T> {

    private final static Logger logger = LoggerFactory.getLogger(RedisTaskCounter.class);


    private final RedisParallelController controller;

    private final String lockName;

    public RedisTaskCounter(RedisParallelController controller, String lockName) {
        this.controller = controller;
        this.lockName = lockName;
    }

    public void release(String domain) {
        try {
            controller.release(lockName, domain);
            logger.debug("释放资源 {}", domain);
        } catch (Exception e) {
            logger.warn("释放资源失败!!!", e.toString());
        }
    }

    @Override
    public void releaseBatch(List<String> domainList) {
        if (null == domainList || domainList.isEmpty()) {
            return;
        }
        Map<String, Integer> domainCount = new HashMap<>();
        for (String domain : domainList) {
            Integer count = domainCount.getOrDefault(domain, 0);
            domainCount.put(domain, count + 1);
        }
        controller.release(lockName, domainCount);
    }

    public T checkAvailable(String key, int max, T task) {
        return null;
    }

    @Override
    public T getNextTaskFromQueue(String queueName) {
        return null;
    }

    @Override
    public Map<String, Integer> checkAvailable(Map<List<T>, Integer> batchMap) {

        Map<String, String> taskDomainCount = new HashMap<>();

        for (Map.Entry<List<T>, Integer> entry : batchMap.entrySet()) {
            T task = entry.getKey().get(0);
            taskDomainCount.put(task.getParallelKey(), entry.getKey().size() + "|" + entry.getValue());
        }
        String val = controller.checkStable(lockName, taskDomainCount);
        return JSON.parseObject(val, new TypeReference<Map<String, Integer>>() {{
        }});
    }

    @Override
    protected boolean preAddQueue(String queueName, String key, T seedTask) {
        return true;
    }

    public void removeTask(String queueName, List<T> list) {
        if (null == list || list.isEmpty()) {
            return;
        }

        Queue<T> queue = getQueue(queueName);
        queue.removeAll(list);
    }

}
