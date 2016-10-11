package com.adtime.crawl.queue.center;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by Lubin.Xuan on 2015/7/29.
 * ie.
 */
public abstract class DomainEntity<P> extends QueueTask<P> implements DomainInter {
    protected String taskUrl;
    @JSONField(serialize = false, deserialize = false)
    protected String domain;

    @Override
    public String getParallelKey() {
        return getDomain();
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTaskUrl() {
        return taskUrl;
    }

    public void setTaskUrl(String taskUrl) {
        this.taskUrl = taskUrl;
    }
}
