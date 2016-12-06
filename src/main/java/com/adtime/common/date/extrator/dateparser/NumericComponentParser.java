/*
 * simple class for parsing numeric components
 *
 * @version $Id: NumericComponentParser.java,v 1.5 2006/10/20 20:53:32 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.dateparser;

import java.util.Map;

public class NumericComponentParser extends DateComponentParser {
	public NumericComponentParser(Map args) {
		super(args);
	}

	public String parse(String dateComponent) {
		/*
		 * TODO: figure out how to get logging to work for component match
		 */
		String trimStr = null;
		int result = -1;

		/* didn't find anything, return -1 */
		if (dateComponent != null
				&& !(trimStr = dateComponent.trim()).equals("")) {

			/* strip off leading zeroes */
			int len = trimStr.length();
			int i;
			for (i = 0; i < len; i++)
				if (trimStr.charAt(i) != '0')
					break;

			if (i == len)/* all zeroes! */
				result = 0;
			else
				result = Integer.parseInt(trimStr.substring(i, len));
		}

		return String.valueOf(result);
	}
}
