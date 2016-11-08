package com.adtime.http.parallel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2015/11/13.
 */
public class ParallelDynamicUpdaterJdbc extends ParallelDynamicUpdaterEmpty {

    private static final Logger logger = LoggerFactory.getLogger(ParallelDynamicUpdaterJdbc.class);

    private DataSource dataSource;

    public ParallelDynamicUpdaterJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
        if (null != this.dataSource) {
            this.init();
        }
    }

    private void init() {
        new Timer("domain_parallel_updater").schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateAllParallelInfo();
                } catch (Throwable e) {
                    logger.error("域名控制信息更新失败!!", e);
                }
            }
        }, 0, 30000);
    }

    /**
     * 字段要求:
     *
     * @see com.adtime.http.parallel.ParallelDynamicUpdater#DOMAIN_KEY 域名
     * @see com.adtime.http.parallel.ParallelDynamicUpdater#PARALLEL_KEY 并发数
     * @see com.adtime.http.parallel.ParallelDynamicUpdater#CONNECTION_TIMEOUT_KEY 连接超时
     * @see com.adtime.http.parallel.ParallelDynamicUpdater#READ_TIMEOUT_KEY 读取超时
     */
    private static final String SQL = "select * from parallel_controller";

    private void updateAllParallelInfo() throws SQLException {
        Connection connection = null;
        PreparedStatement psmt = null;
        ResultSet rst = null;
        try {
            connection = dataSource.getConnection();
            psmt = connection.prepareStatement(SQL);
            rst = psmt.executeQuery();
            while (rst.next()) {
                String domain = rst.getString(DOMAIN_KEY);
                Integer parallel = getValue(rst, PARALLEL_KEY);
                Integer connectionTimeout = getValue(rst, CONNECTION_TIMEOUT_KEY);
                Integer readTimeout = getValue(rst, READ_TIMEOUT_KEY);
                update(domain, new Integer[]{parallel, connectionTimeout, readTimeout});
            }
        } finally {
            close(rst);
            close(psmt);
            close(connection);
        }
    }

    private void close(AutoCloseable autoCloseable) {
        try {
            autoCloseable.close();
        } catch (Exception ignore) {
        }
    }


    public Integer getValue(ResultSet rst, String key) throws SQLException {
        if (null != rst.getObject(key)) {
            return rst.getInt(key);
        } else {
            return null;
        }
    }
}
