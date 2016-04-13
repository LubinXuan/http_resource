package com.adtime.http.resource.http;

import com.adtime.http.resource.Request;
import com.adtime.http.resource.Result;
import com.adtime.http.resource.WebConst;
import com.adtime.http.resource.exception.DownloadStreamException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by Administrator on 2016/4/12.
 */
public class AsyncHttpClient extends HttpClientBaseOperator {
    private CloseableHttpAsyncClient httpAsyncClient;

    public AsyncHttpClient(HttpClientHelper httpClientHelper) throws IOReactorException {
        super(httpClientHelper);
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
        PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(ioReactor);
        cm.setMaxTotal(512);
        HttpAsyncClientBuilder httpAsyncClientBuilder = httpClientHelper.createHttpAsyncClientBuilder();
        httpAsyncClient = httpAsyncClientBuilder.setConnectionManager(cm).build();
        httpAsyncClient.start();
    }

    private static class ConsumerSupplier implements Supplier<Result>, Consumer<Result> {
        Result result = null;
        CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void accept(Result result) {
            this.result = result;
            latch.countDown();
        }

        @Override
        public Result get() {
            try {
                latch.await();
                return result;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Result request(String url, String oUrl, Request request) {
        ConsumerSupplier consumerSupplier = new ConsumerSupplier();
        async(url, oUrl, 0, request, consumerSupplier);
        return consumerSupplier.get();
    }

    @Override
    public void registerCookie(String domain, String name, String value) {

    }

    public void async(String url, String oUrl, int redirect, Request request, Consumer<Result> resultConsumer) {
        HttpRequestBase requestBase = create(url, request);
        httpAsyncClient.execute(requestBase, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse response) {
                Map<String, List<String>> headerMap = readHeader(request, response);

                Result result;

                int sts = response.getStatusLine().getStatusCode();
                try {
                    if (sts == HttpURLConnection.HTTP_MOVED_PERM || sts == HttpURLConnection.HTTP_MOVED_TEMP) {
                        result = handleRedirect(response, url);
                        result.setRedirectCount(redirect + 1);
                    } else {
                        if (Request.Method.HEAD.equals(request.getMethod())) {
                            result = new Result(url, sts, "").withHeader(headerMap);
                        } else {
                            if (sts == HttpURLConnection.HTTP_OK) {
                                result = handleSuccess(response, request.getCharSet(), url, request.isCheckBodySize()).withHeader(headerMap);
                            } else if (sts >= 400) {
                                result = handleSuccess(response, request.getCharSet(), url, request.isCheckBodySize()).withHeader(headerMap);
                            } else {
                                result = new Result(url, "", false, sts).withHeader(headerMap);
                            }
                        }
                        result.setRedirectCount(redirect);
                    }
                    resultConsumer.accept(result);
                } catch (Exception e) {
                    failed(e);
                } finally {
                    close(response, requestBase);
                }
            }

            @Override
            public void failed(Exception e) {
                handException(e, url, oUrl);
                Result result;
                if (e instanceof DownloadStreamException) {
                    result = new Result(url, WebConst.DOWNLOAD_STREAM, e.toString());
                } else {
                    result = new Result(url, WebConst.HTTP_ERROR, e.toString());
                }
                result.setRedirectCount(redirect);
                resultConsumer.accept(result);
                close(null, requestBase);
            }

            @Override
            public void cancelled() {
                Result result = new Result(url, -1, "请求取消");
                result.setRedirectCount(redirect);
                resultConsumer.accept(result);
            }
        });
    }


}
