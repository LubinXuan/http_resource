package com.adtime.crawl.queue.center;

/**
 * Created by xuanlubin on 2016/8/11.
 */
public interface QueueFilterService<P> {
    boolean add(P id);

    void remove(P id);

    boolean contains(P id);
}
