package com.adtime.crawl.queue.center;

import java.util.List;

/**
 * Created by xuanlubin on 2016/8/10.
 */
public interface QueuePersistService<P, T extends Identity<P>> {
    void save(T task);

    List<T> listQueueTask();

    void deleteTask(P id);
}
