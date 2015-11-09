package com.adtime.http.resource.url.format;

import com.adtime.http.resource.common.ConfigReader;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lubin.Xuan on 2015/1/4.
 */
public class DefaultUrlFormat implements FormatUrl {

    public final static String DEFAULT_CONFIG = "http/config/urlFormat.json";

    /**
     * Created by Lubin.Xuan on 2015/1/4.
     */
    private static class UrlFormat {
        private String name;
        private Pattern match;
        private Pattern source;
        private String target;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Pattern getMatch() {
            return match;
        }

        public void setMatch(Pattern match) {
            this.match = match;
        }

        public Pattern getSource() {
            return source;
        }

        public void setSource(Pattern source) {
            this.source = source;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public boolean isMatch(String url) {
            Matcher matcher = match.matcher(url);
            return matcher.matches();
        }

        public String format(String url) {
            Matcher matcher = source.matcher(url);
            if (matcher.find()) {
                int c = matcher.groupCount();
                String reUrl = this.target;
                for (int i = 1; i <= c; i++) {
                    reUrl = reUrl.replace("(" + i + ")", matcher.group(i));
                }
                return reUrl;
            }
            return url;
        }

    }


    private List<UrlFormat> urlFormatList;


    public DefaultUrlFormat(String sourceFile) {
        String jsonObject = ConfigReader.readNOE(sourceFile);
        urlFormatList = JSON.parseObject(jsonObject, new TypeReference<List<UrlFormat>>() {
        });
    }

    public DefaultUrlFormat() {
        this(DEFAULT_CONFIG);
    }

    public String format(String url) {
        try {
            if (null == url || url.trim().length() < 1) {
                return url;
            }
            for (UrlFormat urlFormat : urlFormatList) {
                if (urlFormat.isMatch(url)) {
                    return urlFormat.format(url);
                }
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }
}
