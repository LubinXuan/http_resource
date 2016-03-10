package com.adtime.http.resource.proxy;

/**
 * Created by Lubin.Xuan on 2016/2/19.
 */
public interface ProxyCreator<T> {
    public T create(DynamicProxyProvider.ProxyInfo proxyInfo);
}
