package com.adtime.crawl.queue.center;

import com.adtime.crawl.queue.center.jvm.QueueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Created by Lubin.Xuan on 2015/6/17.
 * ie.
 */
public abstract class TaskCounter<P, T extends Identity<P>> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String DEFAULT_QUEUE = "DEFAULT_QUEUE";

    private final Map<String, BlockingQueue<T>> taskQueueMap = new ConcurrentHashMap<>();

    private final AtomicBoolean init = new AtomicBoolean(false);

    private QueuePersistService<P, T> queuePersistService = null;

    private QueueFilterService<P> queueFilterService = null;

    protected Comparator<T> comparator = null;

    private static final ExecutorService saveExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public int revertQueue() {
        return revertQueue(null);
    }

    public int revertQueue(Consumer<List<T>> consumer) {
        if (init.compareAndSet(false, true)) {
            if (null != queuePersistService) {
                logger.info("开始恢复队列数据");
                long start = System.currentTimeMillis();
                List<T> taskList = queuePersistService.listQueueTask();
                taskList.forEach(this::toQueueNoStore);
                if (null != consumer) {
                    consumer.accept(taskList);
                }
                logger.info("队列数据恢复完成,共恢复数据{} 耗时:", taskList.size(), System.currentTimeMillis() - start);
                return taskList.size();
            }
        }
        return 0;
    }

    abstract public void release(String domain);

    abstract public void releaseBatch(List<String> domainList);

    abstract public T checkAvailable(String key, int max, T task);

    /**
     * @param batchMap key 域名并发数 value 同一域名下任务队列
     * @return
     */
    abstract public Map<String, Integer> checkAvailable(Map<List<T>, Integer> batchMap);

    abstract protected boolean preAddQueue(String queueName, String key, T seedTask);

    public void updateParallel(String domain, int max) {
    }

    ;

    public void toQueue(String queueName, String key, T task) {
        toQueue(queueName, key, task, true);
    }

    public void toQueueNoStore(String queueName, String key, T task) {
        toQueue(queueName, key, task, false);
    }

    private boolean toQueue(String queueName, String key, T task, boolean store) {

        if (null == task) {
            return false;
        }

        if (null != task.getId() && null != queueFilterService) {
            if (!store) {
                try {
                    queueFilterService.refreshFilter(task.getId());
                } catch (Throwable ignore) {
                }
            } else if (!queueFilterService.add(task.getId())) {
                return false;
            }
        }

        task.setQueue(queueName);

        if (store && null != queuePersistService && null != task.getId()) {
            saveExecutor.execute(() -> {
                try {
                    queuePersistService.save(task);
                } catch (Throwable r) {
                    logger.warn("持久化数据异常:{}", r.toString());
                }
            });
        }

        if (!preAddQueue(queueName, key, task)) {
            return true;
        }
        if (null != queueName && queueName.trim().length() > 0) {
            getQueue(queueName.trim()).offer(task);
        } else {
            getQueue(DEFAULT_QUEUE).offer(task);
        }
        return true;
    }

    public boolean toQueue(T seedTask) {
        if (null == seedTask) {
            return false;
        }
        return toQueue(seedTask.getQueue(), seedTask.getParallelKey(), seedTask, true);
    }

    public boolean toQueueNoStore(T seedTask) {
        if (null == seedTask) {
            return true;
        }
        return toQueue(seedTask.getQueue(), seedTask.getParallelKey(), seedTask, false);
    }

    public abstract T getNextTaskFromQueue(String queueName);

    protected BlockingQueue<T> getQueue(String queueName) {
        if (null == queueName || queueName.trim().length() < 1) {
            queueName = DEFAULT_QUEUE;
        }
        return taskQueueMap.compute(queueName, (s, ts) -> {
            if (null == ts) {
                ts = QueueFactory.createQueue(comparator, 1024);
            }
            return ts;
        });
    }

    public Iterator<T> cursor(String queueName) {
        if (null != queueName && queueName.trim().length() > 0) {
            return getQueue(queueName.trim()).iterator();
        } else {
            return getQueue(DEFAULT_QUEUE).iterator();
        }
    }

    public boolean canAddTask(int max) {
        return size() < max;
    }

    public int size() {
        int total = 0;
        for (Queue queue : taskQueueMap.values()) {
            total += queue.size();
        }
        return total;
    }

    public Map<String, Integer> readyQueue() {
        Map<String, Integer> count = new HashMap<>();
        for (Map.Entry<String, BlockingQueue<T>> entry : taskQueueMap.entrySet()) {
            count.put(entry.getKey(), entry.getValue().size());
        }
        return count;
    }

    public void removeTask(String queueName, List<T> list) {
    }

    public void setQueuePersistService(QueuePersistService<P, T> queuePersistService) {
        this.queuePersistService = queuePersistService;
    }

    public void setQueueFilterService(QueueFilterService<P> queueFilterService) {
        this.queueFilterService = queueFilterService;
    }

    public void setComparator(Comparator<T> comparator) {
        this.comparator = comparator;
    }

    public void setComparatorClass(Class<Comparator<T>> comparatorClass) throws IllegalAccessException, InstantiationException {
        this.comparator = comparatorClass.newInstance();
    }
}
