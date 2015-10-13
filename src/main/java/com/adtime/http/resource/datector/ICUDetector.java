package com.adtime.http.resource.datector;

import com.adtime.http.resource.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * Created by Lubin.Xuan on 2015/8/6.
 * ie.
 */
public class ICUDetector extends CharsetDetector {

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
