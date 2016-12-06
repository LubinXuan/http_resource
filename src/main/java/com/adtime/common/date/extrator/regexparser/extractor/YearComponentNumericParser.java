/*
 * simple numeric year parser
 *
 * @version $Id: YearComponentNumericParser.java,v 1.5 2007/02/28 21:36:42 gic Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser.extractor;

import java.util.Map;

public class YearComponentNumericParser extends NumericComponentParser {
	public YearComponentNumericParser(Map args) {
		super(args);
	}

	public String parse(String dateComponent) {
		if (super.log != null)
			super.log.fine("year component is parsed as " + dateComponent);

		String result = super.parse(dateComponent);
		int ret = Integer.parseInt(result);
		if (ret < 0)
			/* error of some sort, just get out */
			return result;

		if (ret < 20) // 0~19 -> 2000~2019
			/* use this century as the default century */
			ret += 2000;
		else if (ret < 100) // 20~99 -> 1920~1999
			ret += 1900;
		// 100~ -> no change

		if (super.log != null)
			super.log.fine("returning " + ret);

		return String.valueOf(ret);
	}
}
