package com.adtime.crawl.queue.center;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by xuanlubin on 2016/8/10.
 */
public class DefaultQueueFilterService<P> implements QueueFilterService<P> {

    private Set<P> taskFilter = new HashSet<>();

    @Override
    public boolean add(P id) {
        return taskFilter.add(id);
    }

    @Override
    public boolean refreshFilter(P id) {
        return true;
    }

    @Override
    public void remove(P id) {
        taskFilter.remove(id);
    }

    @Override
    public boolean contains(P id) {
        return taskFilter.contains(id);
    }
}
