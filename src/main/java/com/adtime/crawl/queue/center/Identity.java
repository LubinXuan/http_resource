package com.adtime.crawl.queue.center;

/**
 * Created by Lubin.Xuan on 2015/8/11.
 * ie.
 */
public interface Identity<T> extends ParallelTask{
    public T getId();
}
