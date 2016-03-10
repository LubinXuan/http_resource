package com.adtime.http;

import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebResource;
import com.adtime.http.resource.proxy.DynamicProxyProvider;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class TestMain extends BaseTest {

    static {
       // System.setProperty("http.proxyHost", "172.16.8.23");
       // System.setProperty("http.proxyPort", "3128");
    }

    private static final Logger logger = LoggerFactory.getLogger(TestMain.class);

    @Resource(name = "webResourceHttpClient")
    //@Resource(name = "webResourceUrlConnection")
    //@Resource(name = "webResourceHtmlUnit")
    private WebResource webResource;


    @Resource
    DynamicProxyProvider dynamicProxyProvider;

    @Test
    public void initProxy() throws IOException {
        Set<String> hostSet = new HashSet<>();
        Set<String> httpsHostSet = new HashSet<>();
        Set<String> urlFilter = new HashSet<>();
        String root = "http://www.youdaili.net/Daili/guonei/4210.html";
        urlFilter.add(root);
        Document document = Jsoup.parse(new URL(root), 3000);
        extractProxyList(document, hostSet, httpsHostSet);
        Elements pages = document.select(".pagelist li a");
        for (Element element : pages) {
            if ("#".equals(element.attr("href"))) {
                continue;
            }
            String url = element.absUrl("href");
            if (StringUtils.isNotBlank(url) && urlFilter.add(url)) {
                document = Jsoup.parse(new URL(url), 3000);
                extractProxyList(document, hostSet, httpsHostSet);
            }
        }
        dynamicProxyProvider.updateProxy(hostSet, httpsHostSet);
    }

    private void extractProxyList(Document document, Set<String> hostSet, Set<String> httpsHostSet) {
        String proxyIp = document.select(".cont_font p").html().toLowerCase();
        String[] hosts = proxyIp.split("<br>");
        for (String h : hosts) {
            int i = h.indexOf("#");
            int j = h.indexOf(":");
            int k = h.indexOf("@");
            if(j<0){
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
    public void testPage(){
        Result result = webResource.fetchPage("http://www.chinaz.com/mobile/2016/0211/503864.shtml?uc_biz_str=S:custom|C:iflow_ncmt|K:true");
        System.out.println();
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

        dynamicProxyProvider.filter(200);

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
    public void countDownLatchTest(){
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(()->{
            try {
                System.out.println("开始等待");
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                latch.countDown();
            }
        }).start();

        try {
            boolean done = latch.await(5,TimeUnit.SECONDS);
            if(done) {
                System.out.println("退出");
            }else{
                System.out.println("超时退出");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
