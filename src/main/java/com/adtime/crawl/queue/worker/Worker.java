package com.adtime.crawl.queue.worker;

/**
 * Created by Lubin.Xuan on 2015/8/21.
 * ie.
 * 爬虫任务处理器 循环执行 出错通知容器重新创建线程
 */
public abstract class Worker<T> {

    /**
     * 获取下一个任务
     *
     * @return 任务实体
     */
    public abstract T getNextTask();

    /**
     * 执行任务
     *
     * @param t 任务实体
     */
    public abstract void startWork(T t);

    /**
     * 异常回调
     *
     * @param t 任务实体
     * @param e 异常信息
     */
    public void handlerThrowable(T t, Throwable e) {
    }

    /**
     * 执行收尾动作
     *
     * @param t 任务实体
     */
    public void onFinal(T t) {
    }
}
