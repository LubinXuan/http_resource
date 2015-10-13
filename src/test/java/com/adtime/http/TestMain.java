package com.adtime.http;

import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebResource;
import com.adtime.tool.DataExtractor;
import com.adtime.tool.util.Type;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class TestMain extends BaseTest {

    @Autowired
    @Qualifier("webResource1")
    private WebResource webResource;


    private Map<String,String> toParamMap(String url){
        Map<String,String> param = new HashMap<>();
        String [] param_pair = url.split("\\?")[1].split("&");
        for (String pair:param_pair){
            int f_i = pair.indexOf("=");
            param.put(pair.substring(0,f_i),pair.substring(f_i+1));
        }
        return param;
    }


    @Test
    public void wx(){
        String url = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxcheckurl?requrl=http%3A%2F%2Fmp.weixin.qq.com%2Fmp%2Fgetmasssendmsg%3F__biz%3DMzA4MDExMTczNQ%3D%3D%23wechat_webview_type%3D1%26wechat_redirect&deviceid=e482620577793569&opcode=2&scene=1";
        Map<String,String> header = new HashMap<>();
        header.put("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36 MicroMessenger/6.5.2.501 NetType/WIFI WindowsWechat");
        header.put("Cookie","webwxuvid=c5ea1e1c32eafe06fdaad29fc31b2e581eac46d2ea73804c568c4477eacec5aeb92da2a593e20620bc46ddb04958530f; wxuin=781277480; wxsid=GKzF22+BlGCpxhUW; webwx_data_ticket=AQbyvZR+PEtMIjf/yt5ivPnQ");

        Result result = webResource.fetchPage(url,null,header);

        Map<String,String> param = toParamMap(result.getMoveToUrl());

        System.out.println();
    }


    @Test
    public void pageFetch(){
        System.setProperty("charset.detector", "ICUDetector");
        String url = "https://login.taobao.com/member/login.jhtml?tpl_redirect_url=https%3A%2F%2Fsec.taobao.com%2Fquery.htm%3Faction%3DQueryAction%26event_submit_do_login%3Dok%26smApp%3Dtmallsearch%26smPolicy%3Dtmallsearch-product-anti_Spider-html-checklogin%26smCharset%3DGBK%26smTag%3DMTIyLjIyNS4xMTQuNTQsLDYzMDg5MjRjOTA4NjQ5YTQ5ODMxOGYwMzhiZTY1MzNh%26smReturn%3Dhttps%253A%252F%252Flist.tmall.com%252Fsearch_product.htm%253Fsearch_condition%253D7%2526style%253Dl%2526sort%253Dd%2526morefilter%253D0%2526q%253D%2525E8%25258A%2525AC%2525E8%2525BE%2525BE%2526s%253D318024%26smSign%3DGVOcK2a1rYPHtiNChLZ1fA%253D%253D&style=miniall&enup=true&full_redirect=true&from=tmall&allp=assets_css%3D2.4.2/login_pc.css%26enup_css%3D2.4.2/enup_pc.css%26assets_js%3D2.4.2/login_performance.js&pms=1439172057";

            url = "http://www.douban.com/group/10197/";

        Result result = webResource.fetchPage(url,null,null,false,10);


        System.out.println();
    }



}
