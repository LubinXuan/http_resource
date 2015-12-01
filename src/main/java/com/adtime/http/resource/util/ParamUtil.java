package com.adtime.http.resource.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2015/10/26.
 */
public class ParamUtil {
    public static class ParamBuilder {
        private Map<String, String> param = new HashMap<>();

        public Map<String, String> get() {
            return param;
        }

        public ParamBuilder add(String key, String val) {
            if (null != key && null != val) {
                param.put(key, val);
            }
            return this;
        }
    }

    public static ParamBuilder builder() {
        return new ParamBuilder();
    }
}
