package com.adtime.http.parallel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2015/11/13.
 */
public class ParallelDynamicUpdaterEmpty implements ParallelDynamicUpdater {

    private static final Integer[] EMPTY = new Integer[]{null, null};

    private Map<String, Integer[]> domainInfoMap = new ConcurrentHashMap<>();

    public Integer parallel(String parallelKey) {
        Integer[] info = domainInfo(parallelKey);
        if (null == info) {
            return null;
        } else {
            return info[0];
        }
    }

    public Integer[] timeout(String domain) {
        Integer[] info = domainInfo(domain);
        if (null == info) {
            return EMPTY;
        } else {
            return new Integer[]{info[1], info[2]};
        }
    }

    private Integer[] domainInfo(String domain) {
        if(domainInfoMap.isEmpty()){
            return null;
        }
        int idx = 0;
        //根据domain 逐级匹配  www.baidu.com->baidu.com->com
        Integer[] match = null;
        while (idx > -1 & idx < domain.length()) {
            match = domainInfoMap.get(domain.substring(idx > 0 ? (idx + 1) : idx));
            if (null != match) {
                break;
            }
            idx = domain.indexOf(".", idx + 1);
        }
        return match;
    }

    public void update(String key, Integer[] info) {
        domainInfoMap.put(key, info);
    }
}
