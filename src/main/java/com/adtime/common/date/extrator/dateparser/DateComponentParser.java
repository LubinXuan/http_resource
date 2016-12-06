/*
 * top-level abstract class for component parsers of a portion of a date
 *
 * @version $Id: DateComponentParser.java,v 1.6 2006/10/20 04:15:58 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.dateparser;

import com.adtime.common.date.extrator.regexparser.BindingCallback;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;


public class DateComponentParser implements BindingCallback {
	/* types of date components that we could extract */
	public static final TreeMap<String, Integer> typeCodes = new TreeMap<String, Integer>();

	public static final TreeMap<Integer, String> typeNames = new TreeMap<Integer, String>();

	/* codes to signify which portion of the date this object returns */
	public static final int SECOND = 0;

	public static final int MINUTE = 1;

	public static final int HOUR = 2;

	public static final int DAY = 3;

	public static final int MONTH = 4;

	public static final int YEAR = 5;

	public static final int TIMEZONE = 6;

	public static final int AM = 7;

	public static final int DELIM_SECOND = 100;

	public static final int DELIM_MINUTE = 101;

	public static final int DELIM_HOUR = 102;

	public static final int DELIM_DAY = 103;

	public static final int DELIM_MONTH = 104;

	public static final int DELIM_YEAR = 105;

	/*
	 * unknown type code goes last so that we can iterate through all type codes
	 * by going up to num_components
	 */
	public static final int UNKNOWN = -1;

	public static final int NUM_COMPONENTS;

	static {
		typeCodes.put("unknown", new Integer(UNKNOWN));
		typeCodes.put("second", new Integer(SECOND));
		typeCodes.put("minute", new Integer(MINUTE));
		typeCodes.put("hour", new Integer(HOUR));
		typeCodes.put("day", new Integer(DAY));
		typeCodes.put("month", new Integer(MONTH));
		typeCodes.put("year", new Integer(YEAR));
		typeCodes.put("timezone", new Integer(TIMEZONE));
		typeCodes.put("am", new Integer(AM));
		typeCodes.put("delim_second", new Integer(DELIM_SECOND));
		typeCodes.put("delim_minute", new Integer(DELIM_MINUTE));
		typeCodes.put("delim_hour", new Integer(DELIM_HOUR));
		typeCodes.put("delim_day", new Integer(DELIM_DAY));
		typeCodes.put("delim_month", new Integer(DELIM_MONTH));
		typeCodes.put("delim_year", new Integer(DELIM_YEAR));

		typeNames.put(new Integer(UNKNOWN), "unknown");
		typeNames.put(new Integer(SECOND), "second");
		typeNames.put(new Integer(MINUTE), "minute");
		typeNames.put(new Integer(HOUR), "hour");
		typeNames.put(new Integer(DAY), "day");
		typeNames.put(new Integer(MONTH), "month");
		typeNames.put(new Integer(YEAR), "year");
		typeNames.put(new Integer(TIMEZONE), "timezone");
		typeNames.put(new Integer(AM), "am");
		typeNames.put(new Integer(DELIM_SECOND), "delim_second");
		typeNames.put(new Integer(DELIM_MINUTE), "delim_minute");
		typeNames.put(new Integer(DELIM_HOUR), "delim_hour");
		typeNames.put(new Integer(DELIM_DAY), "delim_day");
		typeNames.put(new Integer(DELIM_MONTH), "delim_month");
		typeNames.put(new Integer(DELIM_YEAR), "delim_year");

		/* ignore the unknown type code */
		NUM_COMPONENTS = typeNames.size() - 1;
	}

	/*
	 * the name of the date component that this object parses. i.e. 'month-name'
	 * or 'day-numeric'
	 */
	String name;

	/*
	 * the type code indicating what portion of the date this method returns
	 */
	int type;

	Logger log;

	public DateComponentParser(Map map) {
		String typeName = (String) map.get("type");
		this.type = getTypeFromName(typeName);
		this.name = name;
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public int getType() {
		return this.type;
	}

	public static int getTypeFromName(String typeName) {
		if (typeName == null)
			return UNKNOWN;

		Integer type = (Integer) typeCodes.get(typeName.toLowerCase());
		if (type == null)
			return UNKNOWN;
		else
			return type.intValue();
	}

	public static String getNameFromType(int type) {
		if (type >= typeNames.size())
			return getNameFromType(UNKNOWN);
		else
			return (String) typeNames.get(new Integer(type));
	}

	/* return an string corresponding to the portion of the parsed date */
	public String parse(String match) {
		String trimStr = null;
		if (match == null || (trimStr = match.trim()).equals(""))
			return null;

		return trimStr;
	}

	/*
	 * the callback that is expected by the main DateParser engine. Takes in a
	 * string match and a ctx, and mutates that context appropriately
	 */
	public boolean processMatch(String match, Object objCtx) {
		DateParsingContext ctx = (DateParsingContext) objCtx;

		String parseStr = parse(match);
		int nVal = -1;
		if (this.type >= SECOND && this.type <= AM) {
			nVal = Integer.parseInt(parseStr);
			if (nVal == -1)
				return true;
		} else if (parseStr == null)
			return true;

		switch (this.type) {
		case SECOND:
			ctx.setSec(nVal);
			break;
		case MINUTE:
			ctx.setMin(nVal);
			break;
		case HOUR:
			ctx.setHour(nVal);
			break;
		case DAY:
			ctx.setDay(nVal);
			break;
		case MONTH:
			ctx.setMonth(nVal);
			break;
		case YEAR:
			ctx.setYear(nVal);
			break;
		case TIMEZONE:
			ctx.setTimezone(nVal);
			break;
		case AM:
			ctx.setAm(nVal);
			break;
		case DELIM_SECOND:
			ctx.setSecondDelim(parseStr);
			break;
		case DELIM_MINUTE:
			ctx.setMinuteDelim(parseStr);
			break;
		case DELIM_HOUR:
			ctx.setHourDelim(parseStr);
			break;
		case DELIM_DAY:
			ctx.setDayDelim(parseStr);
			break;
		case DELIM_MONTH:
			ctx.setMonthDelim(parseStr);
			break;
		case DELIM_YEAR:
			ctx.setYearDelim(parseStr);
			break;
		default:
			return false;
		}

		return true;
	}
}
