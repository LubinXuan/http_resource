package com.adtime.common.date.extrator.dateparser;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lubin.Xuan on 2014/11/12.
 */
public class DateParserPreFixHandler {

    private static final String ANNOTATED_REGULAR_EXPRESSION_BINDING_NAME = "binding_name";
    private static final String ANNOTATED_REGULAR_EXPRESSION_BINDING_VALUE = "binding_value";
    private static final String ANNOTATED_REGULAR_EXPRESSION_OFF = "off";

    private static class BindHandler {
        String bindName;
        String bindValue;
        int off;
        Pattern pattern;

        public Matcher match(String matchStr) {
            return pattern.matcher(matchStr);
        }
    }

    private static List<BindHandler> bindHandlerList = new ArrayList<BindHandler>();

    public static void init(JSONArray preFix) {
        if (null != preFix && preFix.size() > 0) {
            for (int i = 0; i < preFix.size(); i++) {
                JSONObject item = preFix.getJSONObject(i);
                BindHandler bindHandler = new BindHandler();
                bindHandler.bindName = item.getString(ANNOTATED_REGULAR_EXPRESSION_BINDING_NAME);
                bindHandler.bindValue = item.getString(ANNOTATED_REGULAR_EXPRESSION_BINDING_VALUE);
                bindHandler.off = item.getIntValue(ANNOTATED_REGULAR_EXPRESSION_OFF);
                if (null != bindHandler.bindValue) {
                    bindHandler.pattern = Pattern.compile(bindHandler.bindValue);
                    bindHandlerList.add(bindHandler);
                }
            }
        }
    }

    public static void handle(String prefix, Object objCtx) {
        if (null == prefix || prefix.trim().length() == 0) {
            return;
        }
        prefix = prefix.trim();
        try {
            DateParsingContext ctx = (DateParsingContext) objCtx;
            for (BindHandler bh : bindHandlerList) {
                Matcher matcher = bh.match(prefix);
                if (matcher.matches()) {
                    fillBindHandler(ctx, bh);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void fillBindHandler(DateParsingContext ctx, BindHandler bh) {
        Calendar calendar = Calendar.getInstance();

        int NUM_MILLIS_IN_AN_HOUR = 3600000;
        String[] ids = TimeZone.getAvailableIDs(DateComponentParser.TIMEZONE * NUM_MILLIS_IN_AN_HOUR);
        TimeZone tz;
        if (ids.length == 0) {
            tz = TimeZone.getDefault();
        } else {
            tz = new SimpleTimeZone(DateComponentParser.TIMEZONE * NUM_MILLIS_IN_AN_HOUR, ids[0]);
        }
        calendar.setTimeZone(tz);
        calendar.add(Calendar.DATE, bh.off);
        if (ctx.year <= 0) {
            ctx.year = calendar.get(Calendar.YEAR);
        }
        if (ctx.month <= 0) {
            ctx.month = calendar.get(Calendar.MONTH) + 1;
        }
        if (ctx.day < 0) {
            ctx.day = calendar.get(Calendar.DAY_OF_MONTH);
        }
    }
}
