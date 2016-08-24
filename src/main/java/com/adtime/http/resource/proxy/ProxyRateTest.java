package com.adtime.http.resource.proxy;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by Lubin.Xuan on 2016/2/22.
 */
public class ProxyRateTest {

    private static final String cmd_suffix, encode;
    private static final boolean win;

    static {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("win")) {
            cmd_suffix = " -n 10";
            encode = "gb2312";
            win = true;
        } else {
            cmd_suffix = " -c 10";
            encode = "utf-8";
            win = false;
        }
    }

    public static int test(String host) {
        try {
            Process ps = Runtime.getRuntime().exec("ping " + host + cmd_suffix);
            InputStreamReader isr = new InputStreamReader(ps.getInputStream(), encode);
            BufferedReader in = new BufferedReader(isr);
            String line, lastLine = "";
            while ((line = in.readLine()) != null) {
                lastLine = line;
            }
            in.close();
            int time;
            if (win) {
                int idx = lastLine.lastIndexOf("=");
                time = Integer.parseInt(lastLine.substring(idx + 1).replace("ms", "").trim());
            } else {
                int idx = lastLine.lastIndexOf("=");
                String[] s = lastLine.substring(idx + 1).split("/");
                time = Integer.parseInt(s[1]);
            }
            return time;
        } catch (Throwable e) {
            return -1;
        }
    }

    public static void main(String[] args) {
        System.out.println(test("61.175.231.237"));
    }
}
