package com.adtime.http;

import com.adtime.http.resource.HttpIns;
import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebResource;

/**
 * Created by Administrator on 2015/11/18.
 */
public class Main {
    public static void main(String[] args) {
        WebResource resource = HttpIns.httpClient();

        Result result = resource.fetchPage("https://login.weixin.qq.com/qrcode/AeaeYyz2QQ==",null,null,true);

        System.out.println();
    }
}
