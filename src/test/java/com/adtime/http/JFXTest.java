package com.adtime.http;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;

/**
 * Created by xuanlubin on 2016/6/23.
 */
public class JFXTest extends Application {

    private static final Logger logger = LoggerFactory.getLogger(JFXTest.class);

    private static final Object monitor = new Object();

    private Stage stage;

    private Queue<WebView> viewQueue = new LinkedBlockingQueue<>();

    private Map<String, Integer> loadCount = new ConcurrentHashMap<>();

    @Override
    public void start(Stage primaryStage) throws Exception {

        this.stage = primaryStage;

        Group group = new Group();//作为根节点，也就是root
        Scene scene = new Scene(group);
        primaryStage.setScene(scene);

        int widthTotal = 1200;
        int views = Integer.parseInt(getParameters().getNamed().getOrDefault("views", "1"));
        int viewWidth = widthTotal / views;

        primaryStage.setWidth(widthTotal + 10);
        primaryStage.setHeight(650);
        HBox hbox = new HBox();
        for (int i = 0; i < views; i++) {
            WebView webView = new WebView();
            webView.setId("web-view-" + (i + 1));
            webView.setMinSize(viewWidth, 600);
            webView.setMaxSize(viewWidth, 600);
            final WebEngine engine = webView.getEngine();

            engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                @Override
                public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                    if (Worker.State.SUCCEEDED.equals(newValue)) {
                        Document document = engine.getDocument();
                        JFXHtmlUtils.asFile(document, engine.getLocation());
                        loadCount.compute(webView.getId(), new BiFunction<String, Integer, Integer>() {
                            @Override
                            public Integer apply(String id, Integer integer) {
                                return null == integer ? 1 : integer + 1;
                            }
                        });
                        viewQueue.offer(webView);
                        synchronized (monitor) {
                            monitor.notify();
                        }
                    }
                }

            });


            hbox.getChildren().add(webView);
            viewQueue.offer(webView);
        }

        group.getChildren().add(hbox);

        primaryStage.show();

        Task<UrlLoadTask> urlLoadTask = new Task<UrlLoadTask>() {

            @Override
            protected UrlLoadTask call() throws Exception {
                String[] urls = JFXProvider.privide();
                Set<String> filter = new HashSet<>();
                for (int i = 0, length = urls.length; i < length; i++) {
                    if (filter.add(urls[i])) {
                        updateValue(new UrlLoadTask(i + 1, urls[i]));
                        synchronized (monitor) {
                            monitor.wait();
                        }
                    }
                }
                updateValue(new UrlLoadTask(-1, "加载完成,共加载页面:" + filter.size()));
                logger.info("{}", loadCount);
                return null;
            }
        };


        urlLoadTask.valueProperty().addListener(new ChangeListener<UrlLoadTask>() {
            @Override
            public void changed(ObservableValue<? extends UrlLoadTask> observable, UrlLoadTask oldValue, UrlLoadTask newValue) {
                if (null == newValue) {
                    return;
                }
                WebView view = viewQueue.poll();

                if (newValue.getId() <= 0) {
                    stage.setTitle(newValue.getUrl());
                } else {
                    stage.setTitle(newValue.getId() + ":" + newValue.getUrl());
                    view.getEngine().load(newValue.getUrl());
                }
            }
        });

        new Thread(urlLoadTask).start();


    }

    private static class UrlLoadTask {
        private int id;
        private String url;

        public UrlLoadTask(int id, String url) {
            this.id = id;
            this.url = url;
        }

        public int getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }
    }
}
