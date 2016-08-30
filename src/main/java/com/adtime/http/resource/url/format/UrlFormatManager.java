package com.adtime.http.resource.url.format;

import com.adtime.http.resource.Request;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuanlubin on 2016/8/30.
 */
public class UrlFormatManager<T> {
    private List<UrlFormat<T>> urlFormatList = new ArrayList<>();

    public void updateRequestParam(Request request, T t) {

        if (urlFormatList.isEmpty()) {
            return;
        }

        for (UrlFormat<T> urlFormat : urlFormatList) {
            if (urlFormat.updateRequestParam(request, t)) {
                return;
            }
        }
    }

    public String format(String url, T t) {

        if (urlFormatList.isEmpty()) {
            return null;
        }

        for (UrlFormat<T> urlFormat : urlFormatList) {
            String _url = urlFormat.format(url, t);
            if (StringUtils.isNotBlank(_url)) {
                return _url;
            }
        }
        return null;
    }

    public void setUrlFormatList(List<UrlFormat<T>> urlFormatList) {
        if (null == urlFormatList || urlFormatList.isEmpty()) {
            return;
        }
        this.urlFormatList.addAll(urlFormatList);
    }

    public void addUrlFormat(UrlFormat<T> urlFormat) {
        if (this.urlFormatList.contains(urlFormat)) {
            return;
        }
        this.urlFormatList.add(urlFormat);
    }
}
