/*
 * Utility class for parsing numbers (stripping off leading zeroes, etc)
 *
 * @version $Id: NumberParser.java,v 1.2 2006/09/27 01:40:10 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser.extractor;

public class NumberParser {
	public static int parse(String numberString) {
		/* strip off leading zeroes */
		int len = numberString.length();
		int i;
		for (i = 0; i < len; i++)
			if (numberString.charAt(i) != '0')
				break;

		if (i == len)
			/* all zeroes! */
			return 0;
		else
			return Integer.parseInt(numberString.substring(i, len));
	}
}
