/*
 * default date parsing context to use for default component parsers
 *
 * @version $Id: DateParsingContext.java,v 1.4 2006/10/20 04:20:54 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser.extractor;

import com.adtime.common.date.extrator.regexparser.Context;
import com.adtime.common.date.extrator.regexparser.ContextKeyProvider;

import java.text.SimpleDateFormat;
import java.util.*;


public class DateParsingContext extends Context {
	public static class DateParserContextKeyProvider implements ContextKeyProvider {
		static Set<String> keys = new HashSet<String>();
		
		static {
			keys.add("hour");
			keys.add("minute");
			keys.add("second");
			keys.add("month");
			keys.add("day");
			keys.add("year");
			keys.add("am");
			keys.add("timezone");
		}
		
		public Set<String> getValidKeys() {
			return keys;
		}

	}

	public DateParsingContext(Map<String, String> defaults) throws Exception {
		super(defaults, new DateParserContextKeyProvider());
	}

	/* get the date as an epoch time */
	public String getResult() {
		int NUM_MILLIS_IN_AN_HOUR = 3600000;
		fillInDefaults();

		int timezone = componentAsInt("timezone");
		String gmtId = "GMT " + (timezone >= 0 ? "+" : "-")
				+ timezone;
		SimpleTimeZone stz = new SimpleTimeZone(timezone, gmtId);
		stz.setRawOffset(timezone * NUM_MILLIS_IN_AN_HOUR);
		GregorianCalendar cal = new GregorianCalendar(stz);

		int year = componentAsInt("year");
		int month = componentAsInt("month");
		int day = componentAsInt("day");
		int hour = componentAsInt("hour");
		int min = componentAsInt("minute");
		int sec = componentAsInt("second");
		/* note that the month is 0-based so we have to subtract 1. */
		cal.set(year, month - 1, day, hour, min, sec);

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(cal.getTime());
	}

	private int componentAsInt(String key) {
		int n = 0;
		try {
			n = Integer.parseInt(this.getComponent(key));
		} catch (NumberFormatException e) {
			try {
				n = Integer.parseInt(this.getDefaultForComponent(key));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		return n;
	}
}
