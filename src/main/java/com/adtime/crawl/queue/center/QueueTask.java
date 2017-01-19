package com.adtime.crawl.queue.center;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Comparator;

/**
 * Created by Lubin.Xuan on 2015/10/9.
 * ie.
 */
public abstract class QueueTask<P> implements Identity<P> {

    @JSONField(serialize = false, deserialize = false)
    private int retry = 0;

    private String queue;

    private int priority = 4;

    @Override
    public String getQueue() {
        return queue;
    }

    @Override
    public void setQueue(String queue) {
        this.queue = queue;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int incrRetryAndGet() {
        return ++retry;
    }

    public static class TaskComparator<P extends QueueTask> implements Comparator<P> {
        @Override
        public int compare(P pre, P aft) {
            if (pre.getPriority() == aft.getPriority()) {
                return 0;
            }
            return pre.getPriority() > aft.getPriority() ? -1 : 1;
        }
    }
}
