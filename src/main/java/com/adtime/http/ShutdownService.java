package com.adtime.http;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by xuanlubin on 2017/1/22.
 */
public class ShutdownService {

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

    public static void register(ShutdownHook hook) {
        HOOK_LIST.add(hook);
    }
}
