package com.adtime.crawl.queue.center.jvm;

import com.adtime.crawl.queue.center.Identity;
import com.adtime.crawl.queue.center.ParallelService;
import com.adtime.crawl.queue.center.TaskCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Lubin.Xuan on 2015/6/17.
 * ie.
 */
public class JvmTaskCounter<P, T extends Identity<P>> extends TaskCounter<P, T> {

    private final static Logger logger = LoggerFactory.getLogger(JvmTaskCounter.class);

    private final Map<String, DNode<T>> DOMAIN_COUNTER = new ConcurrentHashMap<>();

    private final Map<String, ParallelService<T>> schedulerMap = new ConcurrentHashMap<>();

    private ParallelService<T> parallelService;

    public void registerParallelService(String queueName, ParallelService<T> parallelService) {
        schedulerMap.putIfAbsent(queueName, parallelService);
    }

    private DNode<T> newDNode(String domain) {
        return new DNode<>(domain, QueueFactory.createQueue(comparator));
    }

    public void release(String domain) {
        try {
            DNode<T> counter = DOMAIN_COUNTER.get(domain);
            if (null != counter) {
                List<T> t = counter.release();
                checkResult(t);
            }
        } catch (Exception e) {
            logger.warn("释放资源失败!!!", e.toString());
        }
    }

    public void updateStatus(T t) {
        DOMAIN_COUNTER.computeIfPresent(t.getParallelKey(), (s, tdNode) -> {
            tdNode.triggerRun();
            return tdNode;
        });
    }

    @Override
    public T getNextTaskFromQueue(String queueName) {
        T t = null;
        try {
            t = getQueue(queueName).take();
        } catch (InterruptedException e) {
            logger.warn("从队列获取任务发生异常:{}", e);
        }
        if (null != t) {
            updateStatus(t);
        }
        return t;
    }

    @Override
    public void releaseBatch(List<String> domainList) {

    }

    public T checkAvailable(String key, int max, T task) {
        /*if (DOMAIN_COUNTER.containsKey(key)) {
            DNode<T> node = DOMAIN_COUNTER.get(key);
            if (node.nextTask(max)) {
                logger.info("{}", node);
                return task;
            } else {
                logger.info("Task [{}] reject by {}", task, node);
            }
        }*/
        return null;
    }

    @Override
    public Map<String, Integer> checkAvailable(Map<List<T>, Integer> batchMap) {
        return Collections.emptyMap();
    }

    @Override
    protected boolean preAddQueue(String queueName, String key, T seedTask) {
        int max = schedulerMap.getOrDefault(queueName, parallelService).getBatchSize(key);
        DNode<T> node = DOMAIN_COUNTER.compute(key, (s, tdNode) -> {
            if (null == tdNode) {
                return newDNode(key);
            }
            return tdNode;
        });
        List<T> t = node.newTask(seedTask, max);
        checkResult(t);
        return false;
    }

    private void checkResult(List<T> tmpList) {
        if (null == tmpList || tmpList.isEmpty()) {
            return;
        }
        for (T t : tmpList) {
            getQueue(t.getQueue()).offer(t);
        }
    }

    @Override
    public void updateParallel(String domain, int parallelLimit) {
        DOMAIN_COUNTER.computeIfPresent(domain, (k, node) -> {
            node.update(parallelLimit);
            return node;
        });
    }

    public DNode<T> node(String parallelKey) {
        return DOMAIN_COUNTER.get(parallelKey);
    }

    public Collection<DNode> nodeList() {
        return Collections.unmodifiableCollection(DOMAIN_COUNTER.values());
    }

    @Override
    public int size() {
        int ready = super.size();
        for (Map.Entry<String, DNode<T>> nodeEntry : DOMAIN_COUNTER.entrySet()) {
            ready += nodeEntry.getValue().getTotal();
        }
        return ready;
    }

    public void setParallelService(ParallelService<T> parallelService) {
        this.parallelService = parallelService;
    }
}
