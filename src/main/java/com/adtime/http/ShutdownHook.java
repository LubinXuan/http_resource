package com.adtime.http;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by xuanlubin on 2017/1/22.
 */
public class ShutdownHook {

    private static final List<ShutdownHook> HOOK_LIST = new LinkedList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Iterator<ShutdownHook> ite = HOOK_LIST.iterator(); ite.hasNext(); ) {
                ShutdownHook hook = ite.next();
                hook.shutdown();
                ite.remove();
            }
        }));
    }

    private boolean shutdown = false;

    private Thread thread;

    void shutdown() {
        this.shutdown = true;
        if (null != thread) {
            this.thread.interrupt();
        }
    }

    public boolean isShutdown() {
        if (null == this.thread) {
            this.thread = Thread.currentThread();
            HOOK_LIST.add(this);
        }
        return shutdown;
    }
}
