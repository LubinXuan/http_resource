package com.adtime.http;

import com.adtime.http.resource.Request;
import com.adtime.http.resource.RequestBuilder;
import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebResource;
import com.adtime.http.resource.util.ParamUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.parser.XmlTreeBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class TestMain extends BaseTest {

    @Autowired
    @Qualifier("webResourceUrlConnection")
    private WebResource webResource;


    private Map<String, String> toParamMap(String url) {
        Map<String, String> param = new HashMap<>();
        String[] param_pair = url.split("\\?")[1].split("&");
        for (String pair : param_pair) {
            int f_i = pair.indexOf("=");
            param.put(pair.substring(0, f_i), pair.substring(f_i + 1));
        }
        return param;
    }


    private String getCheckedUrl(String biz, Map<String, String> header) throws Exception {
        String url = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxcheckurl?requrl=http%3A%2F%2Fmp.weixin.qq.com%2Fmp%2Fgetmasssendmsg%3F__biz%3D{biz}%23wechat_webview_type%3D1%26wechat_redirect&deviceid=e482620577793569&opcode=2&scene=1"
                .replace("{biz}", URLEncoder.encode(biz, "utf-8"));
        Request request = webResource.buildRequest(url, null, header, true, true, 0);
        request.setMethod(Request.Method.GET);
        Result result = webResource.fetchPage(request);
        if (result.isRedirect()) {
            return result.getMoveToUrl();
        } else {
            return null;
        }
    }


    static Map<String, String> header = new HashMap<>();

    static {
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36 MicroMessenger/6.5.2.501 NetType/WIFI WindowsWechat");
        Map<String, String> cookies = new HashMap<>();
        cookies.put("webwx_data_ticket", "AQZGhoeUul6nVH4eRPA0aqPd");
        cookies.put("webwxuvid", "c5ea1e1c32eafe06fdaad29fc31b2e58930e59b08184963fec3ff76019a91f48e774024c2317f64cb97f50da688caee1");
        cookies.put("wxloadtime", "1446621131_expired");
        cookies.put("wxpluginkey", "1446599950");
        cookies.put("wxsid", "CrvGdr+MMHVZiiVk");
        cookies.put("wxuin", "781277480");
       /* cookies.put("pgv_pvi", "6665428992");
        cookies.put("pgv_si", "s8713410560");*/
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("; ");
            }
            stringBuilder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        header.put("Cookie", stringBuilder.toString());
        //header.put("Cookie", "pgv_pvi=6665428992; pgv_si=s8713410560; webwxuvid=c5ea1e1c32eafe06fdaad29fc31b2e58930e59b08184963fec3ff76019a91f48e774024c2317f64cb97f50da688caee1; MM_WX_NOTIFY_STATE=1; MM_WX_SOUND_STATE=1; mm_lang=zh_CN; wxloadtime=1446621131_expired; wxpluginkey=1446599950; wxuin=781277480; wxsid=CrvGdr+MMHVZiiVk; webwx_data_ticket=AQZGhoeUul6nVH4eRPA0aqPd");
    }


    private void refreshKey(String biz) throws Exception {
        String checkedUrl = getCheckedUrl(biz, header);
        Map<String, String> param = toParamMap(checkedUrl);
        uin = param.get("uin");
        key = param.get("key");
    }

    private String key, uin;

    private AtomicLong successReq = new AtomicLong();

    @Test
    public void wx() throws Exception {

        String biz = "MjM5ODU0NzEwMg==";

        refreshKey(biz);

        String api = "http://mp.weixin.qq.com/mp/getmasssendmsg?__biz={biz}&uin={uin}&key={key}&f=json&frommsgid=&count=500&x5=0";

        String jsonApi = api.replace("{biz}", biz).replace("{uin}", uin).replace("{key}", key);
        Request request1 = webResource.buildRequest(jsonApi, null, header, true, true, 0);
        request1.setCheckBodySize(false);
        Result result1 = webResource.fetchPage(request1);

        JSONObject jsonObject = JSONObject.parseObject(result1.getHtml());

        JSONArray list = JSON.parseObject(jsonObject.getString("general_msg_list")).getJSONArray("list");


        /*__biz: MzA5NTUxMzEwMQ==
        mid: 400087032
        sn: 16b6409eea79bd8b75b09515e152f3e2
        idx: 1
        scene: 4
        f: json
        is_need_ad: 0
        uin: MTM3Njc5NjgyOQ==
        key: b410d3164f5f798eb1bd5dcc92eca5c324c0ba8a1373746033d0af286aa6012dc538505578c572f4d58019c672317823*/


        int total = 0;

        List<GZHInfo> gzhInfoList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i).getJSONObject("app_msg_ext_info");
            JSONArray sub = item.getJSONArray("multi_app_msg_item_list");
            total++;
            gzhInfoList.add(asGZHInfo(item, 1));
            if (null != sub) {
                for (int j = 0; j < sub.size(); j++) {
                    JSONObject s_i = sub.getJSONObject(j);
                    gzhInfoList.add(asGZHInfo(s_i, 2 + j));
                    total++;
                }
            }
        }

        System.out.println("total : " + total);

        for (GZHInfo info : gzhInfoList) {
            Request req = voteApi(info, uin, key, header);
            getVoteInfo(req, info.getBiz());
        }

        System.out.println();
    }

    private void getVoteInfo(Request request, String biz) throws Exception {
        Result res = webResource.fetchPage(request);
        if (null != res.getHtml()) {
            try {
                TimeUnit.MILLISECONDS.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (res.getHtml().contains("advertisement_info")) {
                if (!res.getHtml().contains("appmsgstat")) {
                    System.err.println("成功请求 " + successReq.get() + " 后无法继续请求");
                    refreshKey(biz);
                    request.getRequestParam().put("key", key);
                    request.getRequestParam().put("uin", uin);
                    getVoteInfo(request, biz);
                } else {
                    successReq.incrementAndGet();
                    System.err.println(res.getHtml());
                }
            } else {
                getVoteInfo(request, biz);
            }
        }
    }

    String numApi = "http://mp.weixin.qq.com/mp/getappmsgext";

    private Request voteApi(GZHInfo item, String uin, String key, Map<String, String> header) {
        Map<String, String> params = ParamUtil.builder()
                .add("__biz", item.getBiz())
                .add("idx", item.getIdx())
                .add("uin", uin)
                .add("key", key)
                .add("mid", item.getMid())
                .add("sn", item.getSn())
                .add("is_need_ad", "0")
                .add("f", "json")
                .add("scene", "4").get();
        Request request = RequestBuilder.buildRequest(numApi, null, header, true, true, 0);
        request.setRequestParam(params);
        return request;
    }

    private GZHInfo asGZHInfo(JSONObject item, int idx) {
        String c_url = StringEscapeUtils.unescapeHtml4(item.getString("content_url"));
        String mid = StringUtils.substringBetween(c_url, "mid=", "&");
        String sn = StringUtils.substringBetween(c_url, "sn=", "&");
        String biz = StringUtils.substringBetween(c_url, "__biz=", "&");
        return new GZHInfo(biz, idx + "", mid, sn, c_url);
    }

    class GZHInfo {
        private String biz;
        private String idx;
        private String mid;
        private String sn;
        private String url;

        public GZHInfo(String biz, String idx, String mid, String sn, String url) {
            this.biz = biz;
            this.idx = idx;
            this.mid = mid;
            this.sn = sn;
            this.url = url;
        }

        public String getBiz() {
            return biz;
        }

        public String getIdx() {
            return idx;
        }

        public String getMid() {
            return mid;
        }

        public String getSn() {
            return sn;
        }

        public String getUrl() {
            return url;
        }
    }

    @Test
    public void pageFetch() {
        System.setProperty("charset.detector", "ICUDetector");
        String url = "https://login.taobao.com/member/login.jhtml?tpl_redirect_url=https%3A%2F%2Fsec.taobao.com%2Fquery.htm%3Faction%3DQueryAction%26event_submit_do_login%3Dok%26smApp%3Dtmallsearch%26smPolicy%3Dtmallsearch-product-anti_Spider-html-checklogin%26smCharset%3DGBK%26smTag%3DMTIyLjIyNS4xMTQuNTQsLDYzMDg5MjRjOTA4NjQ5YTQ5ODMxOGYwMzhiZTY1MzNh%26smReturn%3Dhttps%253A%252F%252Flist.tmall.com%252Fsearch_product.htm%253Fsearch_condition%253D7%2526style%253Dl%2526sort%253Dd%2526morefilter%253D0%2526q%253D%2525E8%25258A%2525AC%2525E8%2525BE%2525BE%2526s%253D318024%26smSign%3DGVOcK2a1rYPHtiNChLZ1fA%253D%253D&style=miniall&enup=true&full_redirect=true&from=tmall&allp=assets_css%3D2.4.2/login_pc.css%26enup_css%3D2.4.2/enup_pc.css%26assets_js%3D2.4.2/login_performance.js&pms=1439172057";

        url = "http://www.douban.com/group/10197/";
        url = "http://price.pcauto.com.cn/market/63083-10148245.html";
        Result result = webResource.fetchPage(url, null, null, false, 10);

        Jsoup.parse(result.getHtml(), "", new Parser(new XmlTreeBuilder()));
        System.out.println();
    }


}
