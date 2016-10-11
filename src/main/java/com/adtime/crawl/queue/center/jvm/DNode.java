package com.adtime.crawl.queue.center.jvm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
    private DNode nxt;

    private int parallelLimit = 1;

    private String domain;

    private BlockingQueue<T> queue = new LinkedBlockingQueue<>();


    protected DNode() {
        this.nxt = this;
    }

    protected DNode(String domain) {
        this();
        this.domain = domain;
    }

    protected synchronized List<T> newTask(T t, int max) {
        this.parallelLimit = max;
        total++;
        queue.offer(t);
        if (inQueue < this.parallelLimit) {
            List<T> tmpList = new ArrayList<>();
            inQueue += queue.drainTo(tmpList, this.parallelLimit - inQueue);
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
            inQueue += queue.drainTo(tmpList, this.parallelLimit - inQueue);
        }
        if (running > 0) {
            running--;
        }
        total--;
        complete++;
        return tmpList;
    }

    public DNode next() {
        return nxt;
    }

    protected void next(DNode nxt) {
        this.nxt = nxt;
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
