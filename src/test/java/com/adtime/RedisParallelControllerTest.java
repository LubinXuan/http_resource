package com.adtime;

import com.adtime.crawl.queue.center.DomainEntity;
import com.adtime.crawl.queue.center.TaskCounter;
import com.adtime.crawl.queue.center.TaskWorker;
import com.adtime.crawl.queue.center.redis.RedisParallelController;
import com.adtime.crawl.queue.center.redis.RedisTaskCounter;
import junit.framework.TestCase;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Lubin.Xuan on 2015/8/11.
 * ie.
 */
public class RedisParallelControllerTest extends TestCase {

    static RedisParallelController controller;

    static {
        try {
            controller = new RedisParallelController("172.16.2.11", 6380);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void test1() throws InterruptedException {
        String lock = "DOMAIN_LOCK";
        String domain = "www.baidu.com";
        int max = 5;

        AtomicLong count = new AtomicLong();

        long start = System.currentTimeMillis();

        for (int i = 0; i < 20000; i++) {
            System.out.println((i + 1) + "----->" + controller.checkStable(lock, domain, max));
            count.incrementAndGet();
        }

        System.out.println(count.get() + "    " + (System.currentTimeMillis() - start));
        controller.release(lock, domain);

    }


    public void test2() throws InterruptedException {
        String lock = "DOMAIN_LOCK";
        String domain = "www.baidu.com";
        int max = 5;

        AtomicLong count = new AtomicLong();

        long start = System.currentTimeMillis();

        List<String> checkList = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            checkList.add((i + 1) + "|" + domain + "|" + max);
        }

        for (int i = 0; i < 20; i++) {
            checkList.add((i + 21) + "|" + domain);
        }

        for (int i = 0; i < 100000; i++) {
            checkList.add((i + 41) + "|" + domain + "|" + max);
        }

        String result = "";//controller.checkStable(lock, checkList);

        System.out.println(count.get() + "    " + (System.currentTimeMillis() - start));
        System.out.println(result);

        for (int i = 0; i < 20; i++) {
            controller.release(lock, domain);
        }

    }


    public void test4() throws InterruptedException {
        String lock = "DOMAIN_LOCK";
        String domain = "www.baidu.com";
        int max = 5;

        AtomicLong count = new AtomicLong();

        long start = System.currentTimeMillis();

        Map<String, Integer> domainMap = new HashMap<>();
        domainMap.put(domain, 30);
        domainMap.put(domain + 2, 30);

        controller.release(lock, domainMap);

    }

    public void test5() throws InterruptedException {
        String lock = "DOMAIN_LOCK";
        String domain = "www.baidu.com";
        Map<String, String> domainMap = new HashMap<>();
        domainMap.put(domain, 100 + "|" + 50);
        domainMap.put(domain + 2, 100 + "|" + 56);

        System.out.println(controller.checkStable(lock, domainMap));

    }

    class Task extends DomainEntity<Integer> {

        private Integer id;

        public Task(Integer id) {
            this.id = id;
        }

        @Override
        public Integer getId() {
            return id;
        }
    }

    public void test3() throws InterruptedException {
        final TaskCounter<Integer, Task> taskTaskCounter = new RedisTaskCounter<>(controller, "TASK_LOCK_TEST");

        final ExecutorService executorService = Executors.newFixedThreadPool(20);

        final Random random = new Random();

        TaskWorker<Task> worker = task -> {
            System.out.println(task.getTaskUrl());
            executorService.execute(() -> {
                try {
                    TimeUnit.SECONDS.sleep(random.nextInt(5));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                taskTaskCounter.release(task.getDomain());
            });
            return true;
        };
        new Thread(() -> {
            for (int i = 0; i < 500; i++) {
                Task task = new Task(i + 1);
                task.setTaskUrl("http://www.baidu.com/test/" + task.getId() + ".html");
                task.setDomain("www.baidu.com" + (i % 7));
                taskTaskCounter.toQueue(TaskCounter.DEFAULT_QUEUE, task.getDomain(), task);
            }
        }).start();

        /*Scheduler scheduler = new RedisScheduler<>(taskTaskCounter, worker, 50);

        new Thread(() -> {
            while (true) {
                scheduler.schedule();
            }
        }).start();


        TimeUnit.DAYS.sleep(1);*/
    }


    public void test7() {
        Random random = new Random();
        Map<String, Map<String, Integer>> data = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Integer> tmp = new HashMap<>();
            for (int j = 0; j < 5; j++) {
                tmp.put(j + "_tmp", random.nextInt(100));
            }
            data.put(i + "_key", tmp);
        }
        Map<String, Map<String, Integer>> tmp = new HashMap<>();
        tmp = new HashMap<>(data);
        data.clear();
        System.out.println();
    }
}