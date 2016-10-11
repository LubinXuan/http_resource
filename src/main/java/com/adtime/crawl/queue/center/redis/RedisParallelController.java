package com.adtime.crawl.queue.center.redis;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Lubin.Xuan on 2015/8/11.
 * ie.
 */
public class RedisParallelController {

    private static final Logger logger = LoggerFactory.getLogger(RedisParallelController.class);

    private String checkSha;
    private String checkShaMulti;
    private String releaseSha;
    private String releaseShaMulti;

    private JedisPool pool;

    public RedisParallelController(String host, int port) throws Exception {
        String checkScriptMulti = read("checkBatch.lua");
        String checkScript = read("checkSingle.lua");
        String releaseScript = read("releaseSingle.lua");
        String releaseScriptMulti = read("releaseMulti.lua");
        JedisPoolConfig config = new JedisPoolConfig();
        config.setBlockWhenExhausted(true);
        config.setBlockWhenExhausted(true);
        config.setMaxIdle(8);
        config.setMaxIdle(8);
        config.setMaxWaitMillis(10000);
        config.setMinEvictableIdleTimeMillis(1800000);
        config.setMinIdle(0);
        config.setNumTestsPerEvictionRun(3);
        config.setSoftMinEvictableIdleTimeMillis(1800000);
        config.setTestOnBorrow(false);
        config.setTestWhileIdle(false);
        config.setTimeBetweenEvictionRunsMillis(-1);
        pool = new JedisPool(config, host, port);
        Jedis jedis = pool.getResource();
        checkSha = jedis.scriptLoad(checkScript);
        checkShaMulti = jedis.scriptLoad(checkScriptMulti);
        releaseSha = jedis.scriptLoad(releaseScript);
        releaseShaMulti = jedis.scriptLoad(releaseScriptMulti);
        returnResource(jedis);
    }

    private String read(String filePath) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("redis.lua/" + filePath);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        br.close();
        is.close();
        return sb.toString();
    }

    private void returnResource(Jedis jedis) {
        if (null != jedis) {
            pool.returnResource(jedis);
        }
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    public boolean checkStable(String lock, String domain, int max) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            Object retVal = jedis.evalsha(checkSha, 3, lock, domain, String.valueOf(max));
            logger.debug("获取资源 eval result: {}", retVal);
            Long val = (Long) retVal;
            return null != val && val == 1;
        } catch (Exception e) {
            logger.error("获取资源异常", e);
            return true;
        } finally {
            returnResource(jedis);
        }
    }

    public String checkStable(String lock, Map<String, String> domainMap) {
        if (null == domainMap || domainMap.isEmpty()) {
            return "";
        }
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            Object retVal = jedis.evalsha(checkShaMulti, 2, lock, JSON.toJSONString(domainMap));
            logger.debug("获取资源 eval result: {}", retVal);
            return (String) retVal;
        } catch (Exception e) {
            logger.error("获取资源异常", e);
            return "{}";
        } finally {
            returnResource(jedis);
        }
    }


    public boolean _release(String lock, String domain) {
        Jedis jedis = pool.getResource();
        try {
            Object retVal = jedis.evalsha(releaseSha, 2, lock, domain);
            logger.debug("释放资源 eval result: {} ", retVal);
            Long val = (Long) retVal;
            return null != val && val == 1;
        } catch (Exception e) {
            logger.error("释放资源异常", e);
        } finally {
            returnResource(jedis);
        }
        return true;
    }

    public boolean release(String lock, String domain) {
        executor.execute(() -> _release(lock, domain));
        return true;
    }

    public boolean _release(String lock, Map<String, Integer> domain) {
        if (null == domain || domain.isEmpty()) {
            return true;
        }

        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            Object retVal = jedis.evalsha(releaseShaMulti, 2, lock, JSON.toJSONString(domain));
            logger.debug("释放资源 eval result: {}", retVal);
            Long val = (Long) retVal;
            return null != val && val == 1;
        } catch (Exception e) {
            logger.error("释放资源异常", e);
            return true;
        } finally {
            returnResource(jedis);
        }
    }

    public boolean release(String lock, Map<String, Integer> domain) {
        executor.execute(() -> _release(lock, domain));
        return true;
    }
}
