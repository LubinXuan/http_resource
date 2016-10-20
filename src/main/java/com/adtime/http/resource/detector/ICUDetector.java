package com.adtime.http.resource.detector;

import com.adtime.http.resource.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public class ICUDetector extends CharsetDetector {

    static {

        //if (StringUtils.isNotBlank(System.getProperty("icu.lite"))) {


        Set<String> icuLite = new HashSet<>();
        icuLite.add("com.ibm.icu.text.CharsetRecog_UTF8");
        icuLite.add("com.ibm.icu.text.CharsetRecog_2022$CharsetRecog_2022CN");
        icuLite.add("com.ibm.icu.text.CharsetRecog_mbcs$CharsetRecog_gb_18030");
        icuLite.add("com.ibm.icu.text.CharsetRecog_mbcs$CharsetRecog_big5");

        try {
            Field ALL_CS_RECOGNIZERS = com.ibm.icu.text.CharsetDetector.class.getDeclaredField("ALL_CS_RECOGNIZERS");
            ALL_CS_RECOGNIZERS.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(ALL_CS_RECOGNIZERS, ALL_CS_RECOGNIZERS.getModifiers() & ~Modifier.FINAL);

            Field recognizer = Class.forName("com.ibm.icu.text.CharsetDetector$CSRecognizerInfo").getDeclaredField("recognizer");
            recognizer.setAccessible(true);
            Object list = ALL_CS_RECOGNIZERS.get(CharsetDetector.class);
            if (list instanceof List) {
                List _list = new LinkedList();
                for (Object o : (List) list) {
                    Object recognizer_o = recognizer.get(o);
                    if (icuLite.contains(recognizer_o.getClass().getName())) {
                        _list.add(o);
                    }
                }
                ALL_CS_RECOGNIZERS.set(null, Collections.unmodifiableList(_list));
            }

        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        // }
    }

    @Override
    public CharsetInfo detect(byte[] data, String defaultCharset) {
        com.ibm.icu.text.CharsetDetector charsetDetector = new com.ibm.icu.text.CharsetDetector();
        charsetDetector.setText(data);
        CharsetMatch[] all = charsetDetector.detectAll();
        if (null != all && all.length > 0) {
            String[] allProp = new String[all.length];
            for (int i = 0; i < all.length; i++) {
                allProp[i] = all[i].getName();
            }
            return new CharsetInfo(all[0].getName(), allProp);
        } else {
            return null;
        }
    }
}
