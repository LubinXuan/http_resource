package com.adtime.http;

import com.adtime.http.resource.HttpIns;
import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebResource;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.parser.XmlTreeBuilder;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class TestMain extends BaseTest {

    @Resource(name = "webResourceHttpClient")
    private WebResource webResource;


    @Test
    public void pageFetch() {
        System.setProperty("charset.detector", "ICUDetector");
        String url = "https://login.taobao.com/member/login.jhtml?tpl_redirect_url=https%3A%2F%2Fsec.taobao.com%2Fquery.htm%3Faction%3DQueryAction%26event_submit_do_login%3Dok%26smApp%3Dtmallsearch%26smPolicy%3Dtmallsearch-product-anti_Spider-html-checklogin%26smCharset%3DGBK%26smTag%3DMTIyLjIyNS4xMTQuNTQsLDYzMDg5MjRjOTA4NjQ5YTQ5ODMxOGYwMzhiZTY1MzNh%26smReturn%3Dhttps%253A%252F%252Flist.tmall.com%252Fsearch_product.htm%253Fsearch_condition%253D7%2526style%253Dl%2526sort%253Dd%2526morefilter%253D0%2526q%253D%2525E8%25258A%2525AC%2525E8%2525BE%2525BE%2526s%253D318024%26smSign%3DGVOcK2a1rYPHtiNChLZ1fA%253D%253D&style=miniall&enup=true&full_redirect=true&from=tmall&allp=assets_css%3D2.4.2/login_pc.css%26enup_css%3D2.4.2/enup_pc.css%26assets_js%3D2.4.2/login_performance.js&pms=1439172057";

        url = "http://www.douban.com/group/10197/";
        url = "http://price.pcauto.com.cn/market/63083-10148245.html";
        url = "http://taizhou.19lou.com/forum-1426-thread-142421448587948805-1-1.html";
        Result result = webResource.fetchPage(url, null, null, false, 10);

        Jsoup.parse(result.getHtml(), "", new Parser(new XmlTreeBuilder()));
        System.out.println();
    }


}
