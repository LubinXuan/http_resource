package com.adtime.http.resource.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Lubin.Xuan on 2015/6/2.
 * ie.
 */
public class ConfigReader {
    public static String read(String filePath) throws IOException {
        InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream(filePath);
        BufferedReader br = new BufferedReader(new InputStreamReader(is,"utf-8"));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }
        br.close();
        is.close();
        return sb.toString();
    }

    public static String readNOE(String filePath) {
        try {
            return read(filePath);
        } catch (Exception e) {
            return "";
        }
    }
}
