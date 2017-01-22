package com.adtime.http;

/**
 * Created by xuanlubin on 2017/1/22.
 */
public class ShutdownHook {

    private boolean shutdown = false;

    private Thread thread;

    boolean setThread(Thread thread) {
        if (null == this.thread) {
            this.thread = thread;
            return true;
        } else {
            return false;
        }
    }

    void shutdown() {
        this.shutdown = true;
        if (null != thread) {
            this.thread.interrupt();
        }
    }

    public boolean isShutdown() {
        if (null == this.thread) {
            this.thread = Thread.currentThread();
            ShutdownService.register(this);
        }
        return shutdown;
    }
}
