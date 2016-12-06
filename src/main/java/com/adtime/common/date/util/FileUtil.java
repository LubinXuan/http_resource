package com.adtime.common.date.util;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.List;

/**
 * Created by Administrator on 2015/12/3.
 */
public class FileUtil {
    public static String getFileContents(InputStream stream) throws Exception {
        List<String> lines = IOUtils.readLines(stream, "utf-8");
        StringBuilder ret = new StringBuilder("");
        if (null != lines && !lines.isEmpty()) {
            for (String line : lines) {
                if (line.startsWith("//"))
                    continue;
                ret.append("\n").append(line);
            }
        }
        return ret.toString();
    }


    public static String getFileContents(String fileLocation) throws Exception {
        return getFileContents(FileUtil.class.getClassLoader().getResourceAsStream(fileLocation));
    }
}
