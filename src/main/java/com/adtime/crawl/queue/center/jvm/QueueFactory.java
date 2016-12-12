package com.adtime.crawl.queue.center.jvm;

import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by xuanlubin on 2016/12/9.
 */
public class QueueFactory {
    public static <T> PriorityQueue<T> createPriorityQueue() {
        return new PriorityQueue<>(128);
    }

    public static <T> LinkedBlockingQueue<T> createBlockingQueue() {
        return new LinkedBlockingQueue<>(Integer.MAX_VALUE);
    }
}
