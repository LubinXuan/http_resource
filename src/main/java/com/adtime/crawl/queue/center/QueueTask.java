package com.adtime.crawl.queue.center;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by Lubin.Xuan on 2015/10/9.
 * ie.
 */
public abstract class QueueTask<P> implements Identity<P> {

    @JSONField(serialize = false, deserialize = false)
    private int retry = 0;

    private String queue;

    @Override
    public String getQueue() {
        return queue;
    }

    @Override
    public void setQueue(String queue) {
        this.queue = queue;
    }

    public int incrRetryAndGet() {
        return ++retry;
    }
}
