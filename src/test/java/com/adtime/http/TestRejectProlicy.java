package com.adtime.http;

import com.adtime.http.resource.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xuanlubin on 2016/8/19.
 */
public class TestRejectProlicy extends BaseTest {

    static {
        //System.setProperty("http.proxyHost", "172.16.8.28");
        //System.setProperty("http.proxyPort", "3128");
    }

    @Resource(name = "webResourceHttpClient")
    private WebResource resource;


    @Test
    public void main() throws InterruptedException, IOException {

        InputStream is = TestRejectProlicy.class.getClassLoader().getResourceAsStream("19lou.txt");
        List<String> urls = IOUtils.readLines(is);
        is.close();

        //resource.disableCookieSupport("f.lexun.com");
        ExecutorService service = Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(50000);
        for (int i = 0; i < 50000; i++) {
            int id = i;
            service.execute(new Runnable() {
                @Override
                public void run() {
                    Request request = RequestBuilder.buildRequest(urls.get(id % urls.size())).setMaxRedirect(5);
                    //request.addParam("_r", System.currentTimeMillis());
                    Result result = resource.fetchPage(request);
                    if (result.isBodyTruncatedWarning()) {
                        System.out.println("页面获取耗时:::" + result.getRequestTime() + "   异常：" + result.getMessage());
                    } else if (!StringUtils.contains(result.getHtml(), "关于19楼")) {
                        System.out.println("页面获取异常:::" + result.getStatus());
                    } else if (result.getStatus() != 200) {
                        System.out.println("页面获取耗时:::" + result.getRequestTime());
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
    }
}
