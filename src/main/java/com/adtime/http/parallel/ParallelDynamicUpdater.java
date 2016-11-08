package com.adtime.http.parallel;

/**
 * Created by Administrator on 2015/11/13.
 */
public interface ParallelDynamicUpdater {

    String DOMAIN_KEY = "domain";
    String PARALLEL_KEY = "parallel";
    String CONNECTION_TIMEOUT_KEY = "connectionTimeout";
    String READ_TIMEOUT_KEY = "readTimeout";

    Integer parallel(String parallelKey);

    Integer[] timeout(String domain);
}
