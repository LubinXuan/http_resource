package com.adtime.common.date.extrator.dateparser;

import java.util.Calendar;

public class ThreadParam {
    private static ThreadLocal<Calendar> calendarThreadLocal = new ThreadLocal<Calendar>() {
        @Override
        protected Calendar initialValue() {
            return Calendar.getInstance();
        }
    };

    public static Calendar getCalendar() {
        return calendarThreadLocal.get();
    }

    public static void setCalendarThreadLocal(Calendar calendar) {
        ThreadParam.calendarThreadLocal.set(calendar);
    }
}
