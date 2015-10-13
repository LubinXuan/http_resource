package com.adtime.http.resource;

import org.jsoup.nodes.Document;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class Page implements Serializable {
    private String url;
    private String html;
    private int redirectCount;
    private String moveToUrl;

    private Date crawlerTime = Calendar.getInstance().getTime();

    private Document document;


    public Page(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public int getRedirectCount() {
        return redirectCount;
    }

    public void setRedirectCount(int redirectCount) {
        this.redirectCount = redirectCount;
    }

    public String getMoveToUrl() {
        return moveToUrl;
    }

    public void setMoveToUrl(String moveToUrl) {
        this.moveToUrl = moveToUrl;
    }


    public Date getCrawlerTime() {
        return crawlerTime;
    }

    public void setCrawlerTime(Date crawlerTime) {
        this.crawlerTime = crawlerTime;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }

}
