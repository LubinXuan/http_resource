package com.adtime.http.resource.util;

/**
 * Created by Administrator on 2015/12/1.
 */
public class CookieInfo {
    private String domainName;
    private String cookieName;
    private String value;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
