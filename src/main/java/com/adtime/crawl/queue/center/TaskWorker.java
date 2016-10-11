package com.adtime.crawl.queue.center;

/**
 * Created by Lubin.Xuan on 2015/7/29.
 * ie.
 */
public interface TaskWorker<T extends ParallelTask> {
    /**
     * 返回任务是否异步执行
     *
     * @param t
     * @return
     */
    public boolean handle(T t);
}
