package com.adtime.crawl.queue.worker;

import com.adtime.bullbat.queue.center.ParallelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Lubin.Xuan on 2015/9/8.
 * ie.
 */
public abstract class ContainerBuilder<T> {

    private static final Logger logger = LoggerFactory.getLogger(ContainerBuilder.class);

    private Map<String, Container<T>> containerMap = new ConcurrentHashMap<>();

    private ParallelService parallelService;

    public ContainerBuilder(ParallelService parallelService) {
        this.parallelService = parallelService;

        init();

        new Timer("ContainerBuilder-ParallelService-Thread").schedule(new TimerTask() {
            @Override
            public void run() {

                init();

                for (Map.Entry<String, Container<T>> entry : containerMap.entrySet()) {
                    String executorPoolName = entry.getKey();
                    int batch = parallelService.executorPoolSize(executorPoolName);
                    if (batch < 1) {
                        entry.getValue().shutdown();
                    } else {
                        entry.getValue().resizeWorker(batch);
                    }
                }
            }
        }, 10000, 10000);
    }

    public Container<T> build(String executorPoolName) {
        return build(executorPoolName, parallelService.executorPoolSize(executorPoolName));
    }

    public Container<T> build(String executorPoolName, int threadSize) {
        if (containerMap.containsKey(executorPoolName)) {
            return containerMap.get(executorPoolName);
        }
        Container<T> container = create(executorPoolName, threadSize);
        containerMap.put(executorPoolName, container);
        logger.debug("容器:{} 初始并发:{} 初始化 完成!!!", executorPoolName, threadSize);
        return container;
    }

    protected abstract Container<T> create(String executorPoolName, int threadSize);

    public abstract void init();

    public Map<String, Container<T>> getContainerMap() {
        return containerMap;
    }
}
