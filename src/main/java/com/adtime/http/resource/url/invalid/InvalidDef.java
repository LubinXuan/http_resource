package com.adtime.http.resource.url.invalid;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Collections;
import java.util.List;

/**
 * Created by Lubin.Xuan on 2015/5/26.
 * ie.
 */
public class InvalidDef {
    @JSONField(name = "url")
    private List<String> urlPart;
    private List<String> suffix;
    private List<String> prefix;
    private List<String> host;

    public List<String> getUrlPart() {
        return null == urlPart ? Collections.emptyList() : urlPart;
    }

    public void setUrlPart(List<String> urlPart) {
        this.urlPart = urlPart;
    }

    public List<String> getSuffix() {
        return null == suffix ? Collections.emptyList() : suffix;
    }

    public void setSuffix(List<String> suffix) {
        this.suffix = suffix;
    }

    public List<String> getPrefix() {
        return null == prefix ? Collections.emptyList() : prefix;
    }

    public void setPrefix(List<String> prefix) {
        this.prefix = prefix;
    }

    public List<String> getHost() {
        return null == host ? Collections.emptyList() : host;
    }

    public void setHost(List<String> host) {
        this.host = host;
    }
}
