package com.adtime.crawl.queue.center;

import java.util.List;
import java.util.function.Function;

/**
 * Created by xuanlubin on 2016/8/10.
 */
public class QueuePersistFilterLocalService<P, T extends Identity<P>> implements QueuePersistService<P, T>, QueueFilterService<P> {

    private QueueFilterService<P> filterService = new DefaultQueueFilterService<>();

    private final DefaultQueuePersistService<P, T> persistService;

    public QueuePersistFilterLocalService(String storeName, Function<String, T> tFunction) {
        persistService = new DefaultQueuePersistService<>(storeName, tFunction);
    }


    @Override
    public void save(T task) {
        persistService.save(task);
    }

    @Override
    public List<T> listQueueTask() {
        return persistService.listQueueTask();
    }

    @Override
    public void deleteTask(P id) {
        persistService.deleteTask(id);
    }

    @Override
    public boolean add(P id) {
        return filterService.add(id);
    }

    @Override
    public boolean refreshFilter(P id) {
        return filterService.refreshFilter(id);
    }

    @Override
    public void remove(P id) {
        filterService.remove(id);
    }

    @Override
    public boolean contains(P id) {
        return filterService.contains(id);
    }

    @Override
    public void close() {
        persistService.close();
    }

    public void setFilterService(QueueFilterService<P> filterService) {
        this.filterService = filterService;
    }
}
