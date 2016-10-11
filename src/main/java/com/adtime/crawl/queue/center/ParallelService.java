package com.adtime.crawl.queue.center;

/**
 * Created by Lubin.Xuan on 2015/8/21.
 * ie.
 */
public interface ParallelService<T> {
    public int getBatchSize(T entity);

    public int getBatchSize(String key);

    public int executorPoolSize(String poolName);

    void update(String key, int parallel);
}
