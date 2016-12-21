package com.adtime.crawl.queue.center.jvm;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Lubin.Xuan on 2015/7/22.
 * ie.
 */
public class DNode<T> implements Serializable {
    private int running = 0;
    private int inQueue = 0;
    private int total = 0;
    private int complete = 0;

    private int parallelLimit = 1;

    private String domain;

    private Queue<T> queue;


    protected DNode(Queue<T> queue) {
        if (null == queue) {
            throw new IllegalArgumentException("队列不允许为null");
        }
        this.queue = queue;
    }

    protected DNode(String domain, Queue<T> queue) {
        this(queue);
        this.domain = domain;
    }

    protected synchronized List<T> newTask(T t, int max) {
        this.parallelLimit = max;
        total++;
        queue.offer(t);
        if (inQueue < this.parallelLimit) {
            List<T> tmpList = takeFromQueue(this.parallelLimit - inQueue);
            inQueue += tmpList.size();
            return tmpList;
        } else {
            return null;
        }
    }

    protected void triggerRun() {
        synchronized (this) {
            running++;
        }
    }

    protected synchronized List<T> release() {
        List<T> tmpList = new ArrayList<>();
        inQueue--;
        if (inQueue < this.parallelLimit) {
            tmpList = takeFromQueue(this.parallelLimit - inQueue);
            inQueue += tmpList.size();
        }
        if (running > 0) {
            running--;
        }
        total--;
        complete++;
        return tmpList;
    }

    public synchronized List<T> queueData() {
        return new ArrayList<>(queue);
    }

    private List<T> takeFromQueue(int size) {
        if (size <= 0) {
            return Collections.emptyList();
        }
        List<T> tmpList = new ArrayList<>();
        while (size-- > 0) {
            T taskFromQueue = queue.poll();
            if (null == taskFromQueue) {
                break;
            }
            tmpList.add(taskFromQueue);
        }
        return tmpList;
    }

    public int getTotal() {
        return total;
    }

    public int getRunning() {
        return running;
    }

    public void update(int parallelLimit) {
        this.parallelLimit = parallelLimit;
    }

    @Override
    public String toString() {
        return "[" + running + "/" + total + "/" + complete + ":" + domain + "]";
    }
}
