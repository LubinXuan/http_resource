package com.adtime.http.resource;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MimeTypeJudge {

    static final List<String> validMimeType = new ArrayList<>();

    public static final String WAP_WML = "text/vnd.wap.wml";
    public static final String WAP2_WML = "application/vnd.wap.xhtml+xml";

    static {
        validMimeType.add("text/html");
        validMimeType.add("text/plain");
        validMimeType.add("application/xhtml+xml");
        validMimeType.add(WAP_WML);
        validMimeType.add(WAP2_WML);
    }

    public static boolean isWml(String contentType) {
        if (null != contentType && contentType.trim().length() > 0 && (contentType.toLowerCase().contains(WAP2_WML) || contentType.toLowerCase().contains(WAP_WML))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValid(Result result) {
        if (null == result || StringUtils.isBlank(result.getHtml())) {
            return false;
        }
        if (null != result.getContentType() && result.getContentType().trim().length() > 0) {
            return validMimeType.stream().filter(mt -> result.getContentType().toLowerCase().contains(mt)).findAny().isPresent();
        } else {
            return true;
        }
    }
}
