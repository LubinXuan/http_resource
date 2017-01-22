package com.adtime.crawl.queue.worker;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xuanlubin on 2017/1/11.
 */
public class NamedThreadFactory implements ThreadFactory {
    private AtomicInteger threadNumber = new AtomicInteger(1);

    private final String prefix;

    private Queue<Integer> idPool = new LinkedList<>();

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    public Thread newThread(Runnable runnable) {
        Integer id = idPool.poll();
        if (null == id) {
            id = threadNumber.getAndIncrement();
        }

        Integer _id = id;
        return new Thread(runnable, prefix + "-" + _id) {
            @Override
            public void run() {
                try {
                    super.run();
                } finally {
                    idPool.offer(_id);
                }
            }
        };
    }
}
