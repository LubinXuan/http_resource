/*
 * simple numeric year parser
 *
 * @version $Id: YearComponentNumericParser.java,v 1.5 2007/02/28 21:36:42 gic Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.dateparser;

import java.util.Map;

public class YearComponentNumericParser extends NumericComponentParser {
	public YearComponentNumericParser(Map args) {
		super(args);
	}

	public String parse(String dateComponent) {

		String value = super.parse(dateComponent);
		int nVal = Integer.parseInt(value);
		if (nVal >= 0) {
			if (nVal < 20) // 0~19 -> 2000~2019
				/* use this century as the default century */
				nVal += 2000;
			else if (nVal < 100) // 20~99 -> 1920~1999
				nVal += 1900;
			// 100~ -> no change
		}/* else error of some sort, just get out */

		return String.valueOf(nVal);
	}
}
