/*
 * default date parsing context to use for default component parsers
 *
 * @version $Id: DateParsingContext.java,v 1.4 2006/10/20 04:20:54 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.dateparser;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class DateParsingContext implements Cloneable {
    Logger log;

    /* per component-default values */
    Map defaults;

    int month; /* 1-12 indexing of the month */

    int day; /* 1-31 indexing of the day */

    int year; /* year (i.e. 2006) */

    int hour; /* 0-23 hour */

    int min; /* 0-59 min */

    int sec; /* 0-59 sec */

    int timezone; /* +- 0-23 time zone offset from GMT */

    int am; /* 0 h24;1:am;2:pm */

    String yearDelim;

    String monthDelim;

    String dayDelim;

    String hourDelim;

    String minuteDelim;

    String secondDelim;

    int defCount = 0;

    int NUM_MILLIS_IN_AN_HOUR = 3600000;

    public DateParsingContext(Map defaults) throws Exception {
        verifyDefaults(defaults);
        this.defaults = defaults;
        reset();
    }

    public void setLogger(Logger log) {
        this.log = log;
    }

    public void reset() {
        month = -1;
        day = -1;
        year = -1;
        hour = -1;
        min = -1;
        sec = -1;
        timezone = -24;
        am = -1;

        yearDelim = null;
        monthDelim = null;
        dayDelim = null;
        hourDelim = null;
        minuteDelim = null;
        secondDelim = null;
    }

    public boolean isSet(int dateComponentCode) {
        switch (dateComponentCode) {
            case DateComponentParser.YEAR:
                return (this.year != -1);
            case DateComponentParser.MONTH:
                return (this.month != -1);
            case DateComponentParser.DAY:
                return (this.day != -1);
            case DateComponentParser.HOUR:
                return (this.hour != -1);
            case DateComponentParser.MINUTE:
                return (this.min != -1);
            case DateComponentParser.SECOND:
                return (this.sec != -1);
            case DateComponentParser.TIMEZONE:
                return (this.timezone != -24);
            case DateComponentParser.AM:
                return (this.am != -1);
            case DateComponentParser.DELIM_YEAR:
                return (this.yearDelim != null);
            case DateComponentParser.DELIM_MONTH:
                return (this.monthDelim != null);
            case DateComponentParser.DELIM_DAY:
                return (this.dayDelim != null);
            case DateComponentParser.DELIM_HOUR:
                return (this.hourDelim != null);
            case DateComponentParser.DELIM_MINUTE:
                return (this.minuteDelim != null);
            case DateComponentParser.DELIM_SECOND:
                return (this.secondDelim != null);
            default:
                return false;
        }
    }

    private void setComponent(int dateComponentCode, String value) {
        int nVal = -1;
        if (dateComponentCode >= DateComponentParser.SECOND
                && dateComponentCode <= DateComponentParser.AM)
            nVal = Integer.parseInt(value);


        switch (dateComponentCode) {
            case DateComponentParser.YEAR:
                this.year = nVal;
                break;
            case DateComponentParser.MONTH:
                this.month = nVal;
                break;
            case DateComponentParser.DAY:
                this.day = nVal;
                break;
            case DateComponentParser.HOUR:
                this.hour = nVal;
                break;
            case DateComponentParser.MINUTE:
                this.min = nVal;
                break;
            case DateComponentParser.SECOND:
                this.sec = nVal;
                break;
            case DateComponentParser.TIMEZONE:
                this.timezone = nVal;
                break;
            case DateComponentParser.AM:
                this.am = nVal;
                break;
            case DateComponentParser.DELIM_YEAR:
                this.yearDelim = value;
                break;
            case DateComponentParser.DELIM_MONTH:
                this.monthDelim = value;
                break;
            case DateComponentParser.DELIM_DAY:
                this.dayDelim = value;
                break;
            case DateComponentParser.DELIM_HOUR:
                this.hourDelim = value;
                break;
            case DateComponentParser.DELIM_MINUTE:
                this.minuteDelim = value;
                break;
            case DateComponentParser.DELIM_SECOND:
                this.secondDelim = value;
                break;
            default:
                return;
        }
        defCount++;
    }

    public String getComponent(int dateComponentCode) {
        String value = null;
        switch (dateComponentCode) {
            case DateComponentParser.YEAR:
                if (!isSet(DateComponentParser.YEAR))
                    value = getDefaultForComponent(DateComponentParser.YEAR);
                else
                    value = String.valueOf(this.year);
                break;
            case DateComponentParser.MONTH:
                if (!isSet(DateComponentParser.MONTH))
                    value = getDefaultForComponent(DateComponentParser.MONTH);
                else
                    value = String.valueOf(this.month);
                break;
            case DateComponentParser.DAY:
                if (!isSet(DateComponentParser.DAY))
                    value = getDefaultForComponent(DateComponentParser.DAY);
                else
                    value = String.valueOf(this.day);
                break;
            case DateComponentParser.HOUR:
                if (!isSet(DateComponentParser.HOUR))
                    value = getDefaultForComponent(DateComponentParser.HOUR);
                else
                    value = String.valueOf(this.hour);
                break;
            case DateComponentParser.MINUTE:
                if (!isSet(DateComponentParser.MINUTE))
                    value = getDefaultForComponent(DateComponentParser.MINUTE);
                else
                    value = String.valueOf(this.min);
                break;
            case DateComponentParser.SECOND:
                if (!isSet(DateComponentParser.SECOND))
                    value = getDefaultForComponent(DateComponentParser.SECOND);
                else
                    value = String.valueOf(this.sec);
                break;
            case DateComponentParser.TIMEZONE:
                if (!isSet(DateComponentParser.TIMEZONE))
                    value = getDefaultForComponent(DateComponentParser.TIMEZONE);
                else
                    value = String.valueOf(this.timezone);
                break;
            case DateComponentParser.AM:
                if (!isSet(DateComponentParser.AM))
                    value = getDefaultForComponent(DateComponentParser.AM);
                else
                    value = String.valueOf(this.am);
                break;
            case DateComponentParser.DELIM_YEAR:
                value = this.yearDelim;
                break;
            case DateComponentParser.DELIM_MONTH:
                value = this.monthDelim;
                break;
            case DateComponentParser.DELIM_DAY:
                value = this.dayDelim;
                break;
            case DateComponentParser.DELIM_HOUR:
                value = this.hourDelim;
                break;
            case DateComponentParser.DELIM_MINUTE:
                value = this.minuteDelim;
                break;
            case DateComponentParser.DELIM_SECOND:
                value = this.secondDelim;
                break;
            default:
                break;
        }

        return value;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getYear() {
        return this.year;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public void setSec(int sec) {
        this.sec = sec;
    }

    public void setTimezone(int timezone) {
        this.timezone = timezone;
    }

    public void setAm(int am) {
        this.am = am;
    }

    public String getDefaultForComponent(int componentCode) {
        return (String) this.defaults.get(DateComponentParser
                .getNameFromType(componentCode));
    }

    public void setDefaultForComponent(int componentCode, String value) {
        if (value != null)
            this.defaults.put(DateComponentParser
                    .getNameFromType(componentCode), value);
    }

    public void verifyDefaults(Map defaults) throws Exception {
        /* all fields must have a default value */
        for (int i = DateComponentParser.SECOND; i <= DateComponentParser.AM; i++) {
            String code = DateComponentParser.getNameFromType(i);
            if (defaults.get(code) == null)
                throw new Exception("no default setting for date field: "
                        + DateComponentParser.getNameFromType(i));
        }
    }

    public DateParsingContext fillInDefaults() {
        /*
         * for those fields that are not set, use the default values that were
		 * configured
		 */
        DateParsingContext thisClone;
        try {
            thisClone = (DateParsingContext) this.clone();
            thisClone.defCount = 0;
        } catch (CloneNotSupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        Calendar calendar = ThreadParam.getCalendar();

        for (int i = DateComponentParser.SECOND; i <= DateComponentParser.AM; i++) {
            if (!isSet(i)) {
                String defaultValue;
                if (i == DateComponentParser.YEAR) {
                    defaultValue = String.valueOf(calendar.get(Calendar.YEAR));
                } else if (i == DateComponentParser.MONTH) {
                    defaultValue = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                } else if (i == DateComponentParser.DAY) {
                    defaultValue = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
                } else if (i == DateComponentParser.HOUR) {
                    defaultValue = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
                } else if (i == DateComponentParser.MINUTE) {
                    defaultValue = String.valueOf(calendar.get(Calendar.MINUTE));
                } else if (i == DateComponentParser.SECOND) {
                    defaultValue = String.valueOf(calendar.get(Calendar.SECOND));
                } else {
                    defaultValue = getDefaultForComponent(i);
                }
                /* set it to the default value */
                thisClone.setComponent(i, defaultValue);
                if (this.log != null)
                    this.log.info("component "
                            + DateComponentParser.getNameFromType(i)
                            + " not set, using default " + defaultValue);
            }
        }

        return thisClone;
    }

    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < DateComponentParser.NUM_COMPONENTS; i++) {
            buff.append(DateComponentParser.getNameFromType(i));
            buff.append(": ");
            if (!isSet(i)) {
                buff.append(getDefaultForComponent(i));
                buff.append(" (default)");
            } else
                buff.append(getComponent(i));

            buff.append("\n");
        }

        return buff.toString();
    }

    /* get the date as an epoch time */
    public long getDate() {
        DateParsingContext newContext = fillInDefaults();

        String gmtId = "GMT " + (newContext.timezone >= 0 ? "+" : "-")
                + newContext.timezone;
        SimpleTimeZone tz = new SimpleTimeZone(newContext.timezone, gmtId);
        tz.setRawOffset(newContext.timezone * NUM_MILLIS_IN_AN_HOUR);
        GregorianCalendar cal = new GregorianCalendar(tz);

        int hour = newContext.hour;
        if (newContext.am == 2 && hour < 12)
            hour += 12;

		/* note that the month is 0-based so we have to subtract 1. */
        cal.set(newContext.year, newContext.month - 1, newContext.day, hour,
                newContext.min, newContext.sec);

        return cal.getTimeInMillis();
    }

    public long getDate(DateParsingContext newContext) {
        String gmtId = "GMT " + (newContext.timezone >= 0 ? "+" : "-")
                + newContext.timezone;
        SimpleTimeZone tz = new SimpleTimeZone(newContext.timezone, gmtId);
        tz.setRawOffset(newContext.timezone * NUM_MILLIS_IN_AN_HOUR);
        GregorianCalendar cal = new GregorianCalendar(tz);

        int hour = newContext.hour;
        if (newContext.am == 2 && hour < 12)
            hour += 12;

		/* note that the month is 0-based so we have to subtract 1. */
        cal.set(newContext.year, newContext.month - 1, newContext.day, hour,
                newContext.min, newContext.sec);

        return cal.getTimeInMillis();
    }

    public String getFormat() {
        StringBuffer buf = new StringBuffer("");
        if (isSet(DateComponentParser.MONTH)) {
            buf.append("mm");
            if (isSet(DateComponentParser.DELIM_MONTH))
                buf.append(monthDelim);
        }
        if (isSet(DateComponentParser.DAY)) {
            buf.append("dd");
            if (isSet(DateComponentParser.DELIM_DAY))
                buf.append(dayDelim);
        }

        if (isSet(DateComponentParser.YEAR)) {
            String tmpStr = "";
            if (year >= 0 && year < 100)
                tmpStr = "yy";
            else
                tmpStr = "yyyy";

            if (isSet(DateComponentParser.DELIM_YEAR))
                buf.insert(0, tmpStr + yearDelim);
            else
                buf.append(tmpStr);
        }

        buf.append(" ");

        if (isSet(DateComponentParser.AM)) {
            if (am == 1)
                buf.append("AM ");
            else if (am == 2)
                buf.append("PM ");
        }

        if (isSet(DateComponentParser.HOUR)) {
            buf.append("HH");
            if (isSet(DateComponentParser.DELIM_HOUR))
                buf.append(hourDelim);
        }

        if (isSet(DateComponentParser.MINUTE)) {
            buf.append("MM");
            if (isSet(DateComponentParser.DELIM_MINUTE))
                buf.append(minuteDelim);
        }

        if (isSet(DateComponentParser.SECOND)) {
            buf.append("SS");
            if (isSet(DateComponentParser.DELIM_SECOND))
                buf.append(secondDelim);
        }

        return buf.toString().trim();
    }

    public boolean isValidDate() {
        if (isSet(DateComponentParser.YEAR)) {
            if (!((year >= 0 && year < 15) || (year >= 1995 && year <= 2015)))
                return false;
        }
        if (isSet(DateComponentParser.MONTH)) {
            if (month < 1 || month > 12)
                return false;
        }
        if (isSet(DateComponentParser.DAY)) {
            if (day < 1 || day > 31)
                return false;
        }
        if (isSet(DateComponentParser.HOUR)) {
            if (hour < 0 || hour > 23)
                return false;
        }
        if (isSet(DateComponentParser.MINUTE)) {
            if (min < 0 || min > 59)
                return false;
        }
        if (isSet(DateComponentParser.SECOND)) {
            if (sec < 0 || sec > 59)
                return false;
        }

        return (isSet(DateComponentParser.MONTH) && isSet(DateComponentParser.DAY));
    }

    public String getFormatDate() {
        int tmpTimeZone = Integer.parseInt(this
                .getComponent(DateComponentParser.TIMEZONE));
        return DateParsingContext.formatDate(tmpTimeZone, this.getDate());
    }

    public Date praseDate() {
        int tmpTimeZone = Integer.parseInt(this
                .getComponent(DateComponentParser.TIMEZONE));
        String[] ids = TimeZone.getAvailableIDs(timezone
                * NUM_MILLIS_IN_AN_HOUR);
        TimeZone tz = null;
        if (ids.length == 0) {
            // if no ids were returned, something is wrong. use default TimeZone
            tz = TimeZone.getDefault();
        } else {
            tz = new SimpleTimeZone(timezone * NUM_MILLIS_IN_AN_HOUR, ids[0]);
        }
        Calendar calendar = Calendar.getInstance(tz);
        calendar.setTimeInMillis(this.getDate());
        return calendar.getTime();
    }


    public Date praseDate(DateParsingContext newContext) {
        int tmpTimeZone = Integer.parseInt(this
                .getComponent(DateComponentParser.TIMEZONE));
        String[] ids = TimeZone.getAvailableIDs(timezone
                * NUM_MILLIS_IN_AN_HOUR);
        TimeZone tz = null;
        if (ids.length == 0) {
            // if no ids were returned, something is wrong. use default TimeZone
            tz = TimeZone.getDefault();
        } else {
            tz = new SimpleTimeZone(timezone * NUM_MILLIS_IN_AN_HOUR, ids[0]);
        }
        Calendar calendar = Calendar.getInstance(tz);
        calendar.setTimeInMillis(this.getDate(newContext));
        return calendar.getTime();
    }

    public static String formatDate(int timezone, long date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone tz = null;

        int NUM_MILLIS_IN_AN_HOUR = 3600000;
        String[] ids = TimeZone.getAvailableIDs(timezone
                * NUM_MILLIS_IN_AN_HOUR);
        if (ids.length == 0) {
            // if no ids were returned, something is wrong. use default TimeZone
            tz = TimeZone.getDefault();
        } else {
            tz = new SimpleTimeZone(timezone * NUM_MILLIS_IN_AN_HOUR, ids[0]);
        }
        sdf.setTimeZone(tz);
        return sdf.format(date);
    }

    public GregorianCalendar getCurrentDate() {
        int tmpTimeZone = Integer.parseInt(this
                .getComponent(DateComponentParser.TIMEZONE));

        String gmtId = "GMT " + (tmpTimeZone >= 0 ? "+" : "-") + tmpTimeZone;
        SimpleTimeZone tz = new SimpleTimeZone(tmpTimeZone, gmtId);
        tz.setRawOffset(tmpTimeZone * NUM_MILLIS_IN_AN_HOUR);

        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTime(new Date());

        return cal;
    }

    public final void setDayDelim(String dayDelim) {
        this.dayDelim = dayDelim;
    }

    public final void setHourDelim(String hourDelim) {
        this.hourDelim = hourDelim;
    }

    public final void setMinuteDelim(String minuteDelim) {
        this.minuteDelim = minuteDelim;
    }

    public final void setMonthDelim(String monthDelim) {
        this.monthDelim = monthDelim;
    }

    public final void setSecondDelim(String secondDelim) {
        this.secondDelim = secondDelim;
    }

    public final void setYearDelim(String yearDelim) {
        this.yearDelim = yearDelim;
    }

    public final int getDefCount() {
        return defCount;
    }
}
