package com.adtime.crawl.queue.center;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Lubin.Xuan on 2016/1/7.
 */
public class ResourceReleaseHelper {

    private final Queue<Task> releaseQueue = new LinkedBlockingQueue<>();

    private final TaskCounter taskCounter;

    boolean enable = true;

    public ResourceReleaseHelper(TaskCounter taskCounter, boolean enable) {
        this.taskCounter = taskCounter;
        this.enable = enable;
        if (!this.enable) {
            return;
        }
        Thread thread = new Thread(() -> {
            while (true) {
                Task task = releaseQueue.poll();
                if (null == task) {
                    synchronized (releaseQueue) {
                        try {
                            releaseQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                if (task.releaseAt <= System.currentTimeMillis()) {
                    taskCounter.release(task.key);
                } else {
                    releaseQueue.offer(task);
                }
            }
        });
        thread.setName("ResourceReleaseHelper-Thread");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void releaseIn(String res, int val, TimeUnit unit) {
        if(!this.enable) {
            taskCounter.release(res);
            return;
        }
        if (val <= 0) {
            taskCounter.release(res);
        } else {
            long releaseIn = System.currentTimeMillis() + unit.toMillis(val);
            releaseQueue.offer(new Task(res, releaseIn));
            synchronized (releaseQueue) {
                releaseQueue.notify();
            }
        }
    }

    public void releaseIn(ParallelTask task, int val, TimeUnit unit) {
        if (null == task) {
            return;
        }
        releaseIn(task.getParallelKey(), val, unit);
    }

    private static class Task {
        String key;
        long releaseAt;

        public Task(String key, long releaseAt) {
            this.key = key;
            this.releaseAt = releaseAt;
        }
    }

    public static void submit(ResourceReleaseHelper helper, ParallelTask task, int rt) {
        if (null == task) {
            return;
        }
        int intervalTime = rt;
        if (intervalTime > 5) {
            intervalTime = 5;
        }
        helper.releaseIn(task.getParallelKey(), intervalTime, TimeUnit.SECONDS);
    }

}
