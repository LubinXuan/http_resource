/*
 * simple class for parsing numeric components
 *
 * @version $Id: NumericComponentParser.java,v 1.5 2006/10/20 20:53:32 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.dateparser;

import java.util.Map;

public class AMComponentParser extends DateComponentParser {
	public AMComponentParser(Map args) {
		super(args);
	}

	public String parse(String dateComponent) {
		/*
		 * TODO: figure out how to get logging to work for component match
		 */
		String trimStr = null;
		int result = -1;

		/* didn't find anything, return "0" */
		if (dateComponent != null
				&& !(trimStr = dateComponent.trim()).equals("")) {

			if (trimStr.equalsIgnoreCase("am")
					|| trimStr.equalsIgnoreCase("\u4e0a\u5348"))
				result = 1;
			else if (trimStr.equalsIgnoreCase("pm")
					|| trimStr.equalsIgnoreCase("\u4e0b\u5348"))
				result = 2;
			else
				result = 0;
		}

		return String.valueOf(result);
	}
}
