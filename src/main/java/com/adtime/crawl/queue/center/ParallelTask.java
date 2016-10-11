package com.adtime.crawl.queue.center;

/**
 * Created by Lubin.Xuan on 2015/9/9.
 * ie.
 */
public interface ParallelTask {
    public String getParallelKey();
    public String getQueue();
    public void setQueue(String queue);
}
