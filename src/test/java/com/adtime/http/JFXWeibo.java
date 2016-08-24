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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by xuanlubin on 2016/6/23.
 */
public class JFXWeibo extends Application {

    private static final Logger logger = LoggerFactory.getLogger(JFXWeibo.class);

    @Override
    public void start(Stage primaryStage) throws Exception {

        Group group = new Group();//作为根节点，也就是root
        Scene scene = new Scene(group);
        primaryStage.setScene(scene);

        int widthTotal = 1200;
        int views = Integer.parseInt(getParameters().getNamed().getOrDefault("views", "1"));
        int viewWidth = widthTotal / views;

        primaryStage.setWidth(widthTotal + 10);
        primaryStage.setHeight(650);
        HBox hbox = new HBox();

        List<Task> taskList = new ArrayList<>();

        for (int i = 0; i < views; i++) {
            WebView webView = new WebView();
            webView.setId("web-view-" + (i + 1));
            webView.setMinSize(viewWidth, 600);
            webView.setMaxSize(viewWidth, 600);
            final WebEngine engine = webView.getEngine();
            final Object monitor = new Object();
            engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                @Override
                public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                    if (Worker.State.SUCCEEDED.equals(newValue)) {
                        logger.info("success load page:{}", engine.getLocation());
                        Notifyer.notify(monitor);
                    }
                }

            });
            engine.load("https://passport.weibo.cn/signin/login?entry=mweibo&res=wel&wm=3349&r=http://m.weibo.cn");
            hbox.getChildren().add(webView);

            Task<String[]> task = new Task<String[]>() {
                @Override
                protected String[] call() throws Exception {
                    nextCommand(monitor, new String[]{"login", "xlb0907@qq.com", "Xlb900907"}, this::updateValue);
                    nextCommand(monitor, new String[]{"openUrl", "http://m.weibo.cn/u/1656809190"}, this::updateValue);
                    //nextCommand(monitor, new String[]{"hideImg"}, this::updateValue);
                    nextCommand(monitor, new String[]{"moreDetail"}, this::updateValue);
                    return null;
                }
            };

            task.valueProperty().addListener(new ChangeListener<String[]>() {
                @Override
                public void changed(ObservableValue<? extends String[]> observable, String[] oldValue, String[] newValue) {
                    if (null == newValue) {
                        return;
                    }
                    switch (newValue[0]) {
                        case "login":
                            String script = "window._user_info = {\"user\":\"" + newValue[1] + "\",\"pwd\":\"" + newValue[2] + "\"};\n" +
                                    "document.querySelector('#loginName').value=_user_info.user;\n" +
                                    "document.querySelector('#loginPassword').value=_user_info.pwd;\n" +
                                    "document.querySelector('#loginAction').click();";
                            engine.executeScript(script);
                            break;
                        case "openUrl":
                            engine.load(newValue[1]);
                            break;
                        case "hideImg":
                            logger.info("隐藏图片标签~~~~");
                            String hideImgScript = "var _jfx_imgs = document.querySelectorAll(\"img\");\n" +
                                    "for ( var i=0;i<_jfx_imgs.length;i++){\n" +
                                    "\t_jfx_imgs[i].style.display='none';\n" +
                                    "}";
                            engine.executeScript(hideImgScript);
                            Notifyer.notify(monitor);
                            break;
                        case "moreDetail":
                            logger.info("点击更多微博~~~~");
                            engine.executeScript("document.querySelector(\"div.more-detail a\").click();");
                            Notifyer.notify(monitor);
                            break;
                    }
                }
            });

            taskList.add(task);
        }


        group.getChildren().add(hbox);

        primaryStage.show();

        for (Task task : taskList) {
            new Thread(task).start();
        }
    }


    private <T> void nextCommand(final Object monitor, T cmd, Consumer<T> consumer) throws InterruptedException {
        Notifyer.wait(monitor);
        consumer.accept(cmd);
    }

}
