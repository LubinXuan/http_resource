package com.adtime.crawl.queue.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Lubin.Xuan on 2015/8/21.
 * ie.
 * 线程处理容器，负责创建任务线程 维护线程存活状态
 */
public class Container<T> {

    private static final Logger logger = LoggerFactory.getLogger(Container.class);

    private int threadSize;

    private final String key;

    private final AtomicInteger activeCount = new AtomicInteger();

    private final AtomicLong complete = new AtomicLong();

    private final ExecutorService poolExecutor;

    private final Runnable runnable;

    private Thread daemon;

    private boolean shutdown = false;

    private final Object resNotify = new Object();

    public Container(int threadSize, String key, Worker<T> worker) {
        this.threadSize = threadSize;
        this.key = key;
        this.poolExecutor = Executors.newCachedThreadPool(new NamedThreadFactory(this.key));

        this.runnable = () -> {
            while (!shutdown) {
                if (activeCount.get() >= threadSize * 2) {
                    logger.debug("{} thread pool is full", key);
                    synchronized (resNotify) {
                        try {
                            resNotify.wait();
                        } catch (InterruptedException ignored) {

                        }
                    }
                    continue;
                }

                T t;
                try {
                    t = worker.getNextTask();
                } catch (InterruptedException e) {
                    continue;
                }
                updateActive(true);
                poolExecutor.execute(() -> {
                    try {
                        worker.startWork(t);
                    } catch (Throwable e) {
                        logger.error("任务处理异常!!  --  {}", e.toString());
                        worker.handlerThrowable(t, e);
                    } finally {
                        synchronized (resNotify) {
                            resNotify.notify();
                        }
                        updateActive(false);
                        worker.onFinal(t);
                    }
                });
            }
            //强制停止正在执行的任务
            poolExecutor.shutdownNow();
        };

        startDaemon();
    }

    private void startDaemon() {
        this.shutdown = false;
        this.daemon = new Thread(this.runnable);
        this.daemon.setName("container-daemon-" + key);
        this.daemon.start();
        logger.debug("容器:{} 轮询线程启动!!", key);
    }


    private void updateActive(boolean active) {
        if (active) {
            activeCount.incrementAndGet();
        } else {
            activeCount.decrementAndGet();
            complete.incrementAndGet();
        }
    }

    public String status() {
        return String.format("%s[%d/%d/%d]", key, activeCount.get(), complete.get(), threadSize);
    }

    public int active() {
        return activeCount.get();
    }

    public synchronized void resizeWorker(int size) {
        int cur = this.threadSize;
        if (size < 1 || size == cur) {
            return;
        }
        logger.debug("容器:{} 并发数调整 {}->{}", key, cur, size);
        this.threadSize = size;
        if (null == daemon) {
            startDaemon();
        }
    }

    public synchronized void shutdown() {
        if (!shutdown) {
            logger.debug("容器:{} 关闭", key);
            shutdown = true;
            if (null != this.daemon) {
                this.daemon.interrupt();
            }
        }
    }
}
