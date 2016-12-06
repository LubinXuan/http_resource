package com.adtime.common.date.util;

import com.adtime.common.date.extrator.dateparser.DateParserMatcher;
import com.adtime.common.date.extrator.dateparser.DateParsingContext;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Administrator on 2015/12/3.
 */
public class DateUtil {

    /**
     * 发布时间底线 2000/1/1
     */
    public static final Date LIMIT_2000;

    static {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2000);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        LIMIT_2000 = calendar.getTime();
    }

    public static Date findDateByBBSStyle(String matchStr, Date preLimit, Date compare) {
        Calendar calendar;
        Date bestDate = null;
        try {
            calendar = Calendar.getInstance();
            BbsTimeAnalyseUtil bbsTimeAnalyseUtil = new BbsTimeAnalyseUtil(matchStr);
            do {
                Date t = bbsTimeAnalyseUtil.getDate(calendar);
                if (null != t && t.before(compare) && t.after(null == preLimit ? LIMIT_2000 : preLimit)) {
                    bestDate = t;
                    break;
                }
            } while (bbsTimeAnalyseUtil.hasNext());
        } catch (Exception ignored) {

        }
        return bestDate;
    }

    public static Date findDateFromText(Date compare, Date preLimit, DateParserMatcher dpm) {
        int preDefCount = 99;
        Date bestDate = null;
        while (dpm.getNextDate()) {
            DateParsingContext dpc = dpm.getDateParsingContextFillDefault();
            if (preDefCount > dpc.getDefCount()) {
                preDefCount = dpc.getDefCount();
                bestDate = null;
            } else {
                continue;
            }
            Date dateTmp = dpc.praseDate();
            /**
             * 发布时间不会超过当前时间 同时限制其不早于2000/1/1
             */
            if (dateTmp.before(compare) && dateTmp.after(null == preLimit ? LIMIT_2000 : preLimit)) {
                bestDate = dateTmp;
            }
        }
        return bestDate;
    }
}
