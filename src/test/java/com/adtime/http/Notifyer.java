package com.adtime.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xuanlubin on 2016/6/24.
 */
public class Notifyer {

    private static final Logger logger = LoggerFactory.getLogger(Notifyer.class);

    public static void notify(final Object monitor) {
        synchronized (monitor) {
            logger.debug("notify {}", monitor);
            monitor.notify();
        }
    }

    public static void notifyAll(final Object monitor) {
        synchronized (monitor) {
            logger.debug("notifyAll {}", monitor);
            monitor.notifyAll();
        }
    }

    public static void wait(final Object monitor) throws InterruptedException {
        synchronized (monitor) {
            logger.debug("wait {}", monitor);
            monitor.wait();
            logger.debug("notified {}", monitor);
        }
    }
}
