package com.adtime.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by xuanlubin on 2016/6/23.
 */
public class JFXJDSearch extends Application {

    private static final Logger logger = LoggerFactory.getLogger(JFXJDSearch.class);

    private int widthTotal = 1200;

    @Override
    public void start(Stage primaryStage) throws Exception {

        Group group = new Group();//作为根节点，也就是root
        Scene scene = new Scene(group);
        primaryStage.setScene(scene);


        primaryStage.setWidth(widthTotal + 10);
        primaryStage.setHeight(650);
        HBox control = new HBox();
        TextField textField = new TextField();
        Button button = new Button();
        button.setText("抓取JD列表");


        textField.setText("1320-5019-5021");
        textField.setMaxSize(200, 24);
        textField.setMinSize(200, 24);
        control.getChildren().add(textField);
        control.getChildren().add(button);


        Map<String, WebView> webViewMap = new HashMap<>();

        TabPane tabPane = new TabPane();

        button.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String cat = textField.getText();
                //列表view

                if (webViewMap.containsKey(cat)) {
                    return;
                }

                webViewMap.computeIfAbsent(cat, s -> initListView(cat, tabPane, webViewMap));
            }
        });

        VBox panel = new VBox();
        panel.setMaxSize(1200, 600);
        panel.setMinSize(1200, 600);

        panel.getChildren().add(control);
        panel.getChildren().add(tabPane);
        panel.setPadding(new Insets(0, 10, 10, 0));
        group.getChildren().add(panel);

        primaryStage.show();
    }


    private WebView initListView(String cat, TabPane tabPane, Map<String, WebView> webViewMap) {

        WebView webView = new WebView();
        webView.setId("web-view-" + cat);
        final WebEngine engine = webView.getEngine();
        engine.setUserAgent("");
        AtomicBoolean complete = new AtomicBoolean(false);

        BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                while (!complete.get()) {
                    Object o = queue.poll(5, TimeUnit.SECONDS);
                    if (null == o) {
                        continue;
                    }
                    TimeUnit.MILLISECONDS.sleep(600);
                    updateValue(UUID.randomUUID().toString());
                }
                return null;
            }
        };

        task.valueProperty().addListener((observable, oldValue, newValue) -> {
            logger.info("执行滚屏动作");
            engine.executeScript("window.scrollTo(0,document.body.scrollHeight)");
        });

        AtomicReference<String> current = new AtomicReference<>();

        BlockingQueue<JSONArray> arrayQueue = new LinkedBlockingQueue<>();

        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED.equals(newValue)) {
                current.set(engine.getLocation());
                logger.info("success load page:{}", engine.getLocation());
                String html = JFXHtmlUtils.getHtml(engine.getDocument());
                String[] lines = html.split("\n");
                String itemArgs = null;
                for (String line : lines) {
                    if (line.contains("jsArgs['search']")) {
                        itemArgs = StringUtils.substringAfter(line, "=").trim();
                        break;
                    }
                }
                if (null == itemArgs) {
                    return;
                }
                JDItemListRspHandler javaScriptCaller = new JDItemListRspHandler(queue, complete, arrayQueue);
                injectAjaxHandler(engine, javaScriptCaller);
                JSONObject data = JSON.parseObject(itemArgs.substring(0, itemArgs.length() - 1));
                javaScriptCaller.handle(data.getJSONObject("data"));
            }
        });

        new Thread(task).start();

        Tab tab = new Tab();
        tab.setText(cat);
        tab.setContent(webView);
        new Thread(() -> {
            while (!complete.get()) {
                try {
                    JSONArray array = arrayQueue.poll(5, TimeUnit.SECONDS);
                    if (null == array) {
                        continue;
                    }
                    List<String> data = new ArrayList<>();
                    for (int i = 0; i < array.size(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        String[] info = new String[7];
                        info[0] = item.getString("wareId");
                        info[1] = item.getString("good");
                        info[2] = item.getString("totalCount");
                        info[3] = item.getString("jdPrice");
                        info[4] = item.getString("wname");
                        info[6] = item.getString("imageurl");
                        Boolean self = item.getBoolean("self");
                        Boolean international = item.getBoolean("international");
                        info[5] = self ? "自营" : international ? "全球购" : "平台";
                        data.add(StringUtils.join(info, "\t"));
                    }
                    String url = current.get();
                    int s = url.lastIndexOf("/");
                    int e = url.indexOf(".", s + 1);
                    FileUtils.write(new File(url.substring(s + 1, e) + ".txt"), StringUtils.join(data, "\n"), true);
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
            logger.info("列表拉取完毕");
            tabPane.getTabs().remove(tab);
            webViewMap.remove(cat);
        }).start();

        tab.setOnClosed(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                complete.set(true);
            }
        });

        tabPane.getTabs().add(tab);
        engine.load("http://so.m.jd.com/products/" + cat + ".html");
        return webView;

    }

    private void injectAjaxHandler(WebEngine engine, JavaScriptCaller javaScriptCaller) {
        JSObject win = (JSObject) engine.executeScript("window");
        String handlerName = "__" + UUID.randomUUID().toString().replaceAll("-", "_");
        win.setMember(handlerName, javaScriptCaller);
        engine.executeScript("var __ajax = $.ajax;\n" +
                "$.ajax = function(opt){\n" +
                "   var l_suc = opt.success;\n" +
                "   opt.success = function(m){\n" +
                "      " + handlerName + ".ajax(opt.url,JSON.stringify(m));\n" +
                "      l_suc&&l_suc(m);\n" +
                "   }\n" +
                "   __ajax(opt);\n" +
                "};");
    }


    public static abstract class JavaScriptCaller {

        final BlockingQueue<Object> queue;
        final AtomicBoolean complete;

        public JavaScriptCaller(BlockingQueue<Object> queue, AtomicBoolean complete) {
            this.queue = queue;
            this.complete = complete;
        }

        public abstract void ajax(String url, String rsp);
    }


    public static final class JDItemListRspHandler extends JavaScriptCaller {

        final BlockingQueue<JSONArray> arrayQueue;

        public JDItemListRspHandler(BlockingQueue<Object> queue, AtomicBoolean complete, BlockingQueue<JSONArray> arrayQueue) {
            super(queue, complete);
            this.arrayQueue = arrayQueue;
        }

        @Override
        public void ajax(String url, String rsp) {
            if (url.contains("/ware/searchList.action")) {
                logger.debug("拦截到商品列表rsp:{} {}", url, rsp);
                JSONObject json = JSON.parseObject(rsp);
                JSONObject value = JSON.parseObject(json.getString("value"));
                handle(value);
            } else {
                logger.info("拦截到AJAX rsp:{} {}", url, rsp);
            }
        }

        void handle(JSONObject itemData) {
            if (null == itemData) {
                this.complete.set(true);
                return;
            }
            JSONArray jsonArray = itemData.getJSONArray("wareList");
            if (!jsonArray.isEmpty()) {
                arrayQueue.offer(jsonArray);
                logger.info("列表中商品数有:{}", jsonArray.size());
                this.queue.offer(this);
            } else {
                this.complete.set(true);
            }
        }
    }
}
