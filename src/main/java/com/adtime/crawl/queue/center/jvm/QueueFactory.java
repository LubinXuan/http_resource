package com.adtime.crawl.queue.center.jvm;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by xuanlubin on 2016/12/9.
 */
public class QueueFactory {

    public static <T> BlockingQueue<T> createQueue(Comparator<T> comparator) {
        return createQueue(comparator, 128);
    }

    public static <T> BlockingQueue<T> createQueue(Comparator<T> comparator, int capacity) {
        return null == comparator ? new LinkedBlockingQueue<>(Integer.MAX_VALUE) : new PriorityBlockingQueue<>(capacity, comparator);
    }
}
