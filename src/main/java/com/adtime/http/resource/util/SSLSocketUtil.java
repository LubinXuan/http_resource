package com.adtime.http.resource.util;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by Lubin.Xuan on 2015/8/10.
 * ie.
 */
public class SSLSocketUtil {

    private static final Logger logger = LoggerFactory.getLogger(SSLSocketUtil.class);

    protected final static SSLContext sslcontext;

    protected static final TrustManager myX509TrustManager = new X509TrustManager() {

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
    };


    static {
        SSLContext _ssl = null;
        try {
            _ssl = SSLContext.getInstance("TLSv1");
            _ssl.init(null, new TrustManager[]{myX509TrustManager}, null);
        } catch (Exception e) {
            logger.error("SSLContext 初始化失败!!!", e);
        }
        sslcontext = _ssl;
    }

    public static SSLContext getSSLContext() {
        return sslcontext;
    }
}
