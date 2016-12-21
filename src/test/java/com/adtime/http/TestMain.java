package com.adtime.http;

import com.adtime.AdslReboot;
import com.adtime.common.date.util.FileUtil;
import com.adtime.http.resource.*;
import com.adtime.http.resource.http.AsyncHttpClient;
import com.adtime.http.resource.proxy.DynamicProxyProvider;
import com.adtime.http.resource.url.URLInetAddress;
import javafx.application.Application;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.nio.reactor.IOReactorException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class TestMain extends BaseTest {

    static {
        //System.setProperty("http.proxyHost","49.74.216.165");
        //System.setProperty("http.proxyPort","20121");
        //System.setProperty("http.proxyHost", "192.168.168.125");
        //System.setProperty("http.proxyPort", "3128");
        //System.setProperty("sun.net.spi.nameservice.provider.1", "dns,xbill");
    }

    private static final Logger logger = LoggerFactory.getLogger(TestMain.class);

    @Resource(name = "asyncHttpClient")
    private WebResource asyncHttpClient;
    @Resource(name = "webResourceUrlConnection")
    //@Resource(name = "webResourceHtmlUnit")
    //@Resource(name = "webResourceHttpClient")
    private WebResource webResource;


    @Resource
    DynamicProxyProvider dynamicProxyProvider;

    @Test
    public void initProxy() throws IOException {
        Set<String> hostSet = new HashSet<>();
        Set<String> urlFilter = new HashSet<>();
        String root = "http://www.youdaili.net/Daili/guonei/4592.html";
        urlFilter.add(root);
        Request request = new Request();
        request.setUrl(root);
        request.setHeader("Cookie", "yunsuo_session_verify=3d0da40a751da1b85d8a19406afaa22a; bdshare_ty=0x18; Hm_lvt_f8bdd88d72441a9ad0f8c82db3113a84=1466475057; Hm_lpvt_f8bdd88d72441a9ad0f8c82db3113a84=1466475717");
        Document document = webResource.fetchPage(request).getDocument();
        extractProxyList(document, hostSet, hostSet);
        Elements pages = document.select(".pagelist li a");
        for (Element element : pages) {
            if ("#".equals(element.attr("href"))) {
                continue;
            }
            String url = element.absUrl("href");
            if (StringUtils.isNotBlank(url) && urlFilter.add(url)) {
                request.setUrl(url);
                request.setHeader("Cookie", "yunsuo_session_verify=3d0da40a751da1b85d8a19406afaa22a; bdshare_ty=0x18; Hm_lvt_f8bdd88d72441a9ad0f8c82db3113a84=1466475057; Hm_lpvt_f8bdd88d72441a9ad0f8c82db3113a84=1466475717");
                document = webResource.fetchPage(request).getDocument();
                extractProxyList(document, hostSet, hostSet);
            }
        }

        dynamicProxyProvider.updateProxy(hostSet.toArray(new String[0]));
    }

    private void extractProxyList(Document document, Set<String> hostSet, Set<String> httpsHostSet) {
        String proxyIp = document.select(".cont_font p").html().toLowerCase();
        String[] hosts = proxyIp.split("<br>");
        for (String h : hosts) {
            int i = h.indexOf("#");
            int j = h.indexOf(":");
            int k = h.indexOf("@");
            if (j < 0) {
                System.out.println(h);
                continue;
            }
            String host = h.substring(0, j).trim();
            String port = h.substring(j + 1, k);
            String scheme = h.substring(k + 1, i);
            String description = h.substring(i + 1);
            String str = scheme + ":" + host + ":" + port + ":" + description;
            if ("http".equals(scheme)) {
                hostSet.add(str);
            } else {
                httpsHostSet.add(str);
            }
        }
    }


    @Test
    public void testPage() {
        //dynamicProxyProvider.updateProxy(new String[]{"https:192.168.168.103:3128","https:172.16.8.23:3128", "https:172.16.8.28:3128","https:172.16.8.40:3128"});
        URLInetAddress.disableHostReplace();
        //dynamicProxyProvider.updateProxy(new String[]{"https:192.168.168.125:3128"});
        for (int i = 0; i < 10; i++) {
            Result result = webResource.fetchPage("http://www.che101.com/biaozhi407 coupe(jinkou)/");
            System.out.println("==================================" + result.getHeadersMap().get("X-Cache-Lookup"));
            System.out.println(result);
        }
    }


    @Test
    public void testAsync() throws IOReactorException, InterruptedException {
        dynamicProxyProvider.updateProxy(new String[]{"https:192.168.168.132:3128"});
        CountDownLatch latch = new CountDownLatch(1000);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            asyncHttpClient.fetchPage("http://news.ifeng.com/a/20160418/48500891_0.shtml", (ResultConsumer) var1 -> {
                logger.debug("访问耗时:{} {}", var1.getRequestTime(), var1.getStatus());
                latch.countDown();
            });
        }
        latch.await();
        logger.debug("耗时:{}", System.currentTimeMillis() - start);
    }

    @Test
    public void testAsync2() throws IOReactorException, InterruptedException {

        String[] urls = {
                "http://tieba.baidu.com/p/4440709622",
                "http://tieba.baidu.com/p/4113492066",
                "http://tieba.baidu.com/p/4821619037",
                "http://tieba.baidu.com/p/4820996081",
                "http://tieba.baidu.com/p/4809890049",
                "http://tieba.baidu.com/p/4818652748"
        };

        webResource.fetchPage("http://news.ifeng.com/a/20160418/48500891_0.shtml", (ResultConsumer) var1 -> {
            logger.info("访问耗时:{} {}", var1.getRequestTime(), var1.getStatus());
        });
        webResource.fetchPage("http://www.ctex.cn/j/web/project.jsp?catEname=/zrxx/jsxm&infoId=20140900039427", (ResultConsumer) var1 -> {
            logger.info("访问耗时:{} {}", var1.getRequestTime(), var1.getStatus());
        });
        webResource.fetchPage("http://www.douban.com/group/10197/", (ResultConsumer) var1 -> {
            logger.info("访问耗时:{} {}", var1.getRequestTime(), var1.getStatus());
        });

        for (String url : urls) {
            webResource.fetchPage(url, (ResultConsumer) var1 -> {
                logger.info("访问耗时:{} {}", var1.getRequestTime(), var1.getStatus());
            });
        }


    }

    @Test
    public void pageFetch() throws IOException, InterruptedException {
        System.setProperty("charset.detector", "ICUDetector");
        String url = "https://login.taobao.com/member/login.jhtml?tpl_redirect_url=https%3A%2F%2Fsec.taobao.com%2Fquery.htm%3Faction%3DQueryAction%26event_submit_do_login%3Dok%26smApp%3Dtmallsearch%26smPolicy%3Dtmallsearch-product-anti_Spider-html-checklogin%26smCharset%3DGBK%26smTag%3DMTIyLjIyNS4xMTQuNTQsLDYzMDg5MjRjOTA4NjQ5YTQ5ODMxOGYwMzhiZTY1MzNh%26smReturn%3Dhttps%253A%252F%252Flist.tmall.com%252Fsearch_product.htm%253Fsearch_condition%253D7%2526style%253Dl%2526sort%253Dd%2526morefilter%253D0%2526q%253D%2525E8%25258A%2525AC%2525E8%2525BE%2525BE%2526s%253D318024%26smSign%3DGVOcK2a1rYPHtiNChLZ1fA%253D%253D&style=miniall&enup=true&full_redirect=true&from=tmall&allp=assets_css%3D2.4.2/login_pc.css%26enup_css%3D2.4.2/enup_pc.css%26assets_js%3D2.4.2/login_performance.js&pms=1439172057";

        url = "http://www.douban.com/group/10197/";
        url = "http://price.pcauto.com.cn/market/63083-10148245.html";
        url = "http://taizhou.19lou.com/forum-1426-thread-142421448587948805-1-1.html";
        //url = "http://www.pajssc.com/jscg/index.jhtml";
        //url = "http://d.news.163.com/articlesPage/shy";
        //url = "http://forum.anywlan.com/thread-380874-1-1.html";
        //url = "https://www.baidu.com";

        initProxy();

        //dynamicProxyProvider.filter(200);

        String _url = url;

        CountDownLatch latch = new CountDownLatch(50);

        AtomicInteger log = new AtomicInteger(0);

        Map<String, Integer> errorCount = new ConcurrentHashMap<>();

        for (int i = 0; i < 1; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        try {
                            Result result = webResource.fetchPage(_url, null, null, false, 0);
                            if (200 != result.getStatus()) {
                                if (503 == result.getStatus()) {
                                    logger.warn("{} {} {} {}获取异常", j + 1, result.getStatus(), result.getMessage(), "!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                                } else {
                                    logger.warn("{} {} {} 获取异常", j + 1, result.getStatus(), result.getMessage());
                                }
                                errorCount.compute(result.getStatus() + result.getMessage(), (s, integer) -> null == integer ? 1 : (integer + 1));
                            } else {
                                Document document = Jsoup.parse(result.getHtml(), "", Parser.htmlParser());
                                String text = document.text();
                                if (StringUtils.isBlank(text)) {
                                    logger.warn("{} {} {}", j + 1, result.getStatus(), "~~~~~~~~");
                                } else {
                                    logger.warn("{} {} {}", j + 1, result.getStatus(), "_______________");
                                }
                                log.incrementAndGet();
                            }
                        } catch (Throwable ignore) {
                            errorCount.compute(-11111 + ignore.getMessage(), (s, integer) -> null == integer ? 1 : (integer + 1));
                        }
                        TimeUnit.SECONDS.sleep(0);
                    }
                } catch (InterruptedException ignored) {

                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();

        logger.debug("成功访问   {}", log.get());

        logger.debug("异常信息:{}", errorCount);
    }


    @Test
    public void countDownLatchTest() {
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                System.out.println("开始等待");
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();

        try {
            boolean done = latch.await(5, TimeUnit.SECONDS);
            if (done) {
                System.out.println("退出");
            } else {
                System.out.println("超时退出");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Resource(name = "webResourceHtmlUnit")
    WebResource webResourceHtmlUnit;

    static {
        System.setProperty("http.proxyHost","49.74.216.165");
        System.setProperty("http.proxyPort","20121");
    }

    @Test
    public void testProxy() {
        Request request = RequestBuilder.buildRequest("");
        request.setMaxRedirect(20);
        Result result = webResourceHtmlUnit.fetchPage(request);

        System.out.println();

    }

    public static void main(String[] args) throws MalformedURLException {
        //Application.launch(JFXTest.class, "--views=10");

        URL url = new URL("http://www.baibu.com/a/b/c?q=3&r=4&p=4#ddd=dsds");

        JFXHtmlUtils.init();
        Application.launch(JFXJDSearch.class, "--views=1");
    }

    @Test
    public void testAdslConnect() throws IOException, InterruptedException {

        //File file = new File("./ip.txt");

        while (true) {
            //AdslReboot.connect();

            Result result = webResource.fetchPage("http://cache.video.qiyi.com/jp/sdvlst/6/1300000720/201512/");
            if (result.getStatus() == 200) {
                String ip = result.getHtml();
                System.out.println(ip);
                //FileUtils.write(file, ip + "\n", "utf-8", true);
            }

            //TimeUnit.SECONDS.sleep(10);
            //AdslReboot.disconnect();
            //TimeUnit.SECONDS.sleep(1);
        }
    }

}
