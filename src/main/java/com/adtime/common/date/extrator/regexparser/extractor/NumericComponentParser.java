/*
 * simple class for parsing numeric components
 *
 * @version $Id: NumericComponentParser.java,v 1.5 2006/10/20 20:53:32 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser.extractor;

import java.util.Map;

public class NumericComponentParser extends ComponentParser {
	public NumericComponentParser(Map args) {
		super(args);
	}

	public String parse(String dateComponent) {
		/*
		 * TODO: figure out how to get logging to work for component match
		 * callbacks System.out.println("type " + getNameFromType(this.type) + "
		 * got match " + dateComponent);
		 */

		/* didn't find anything, return -1 */
		if (dateComponent == null || dateComponent.trim().equals(""))
			return "-1";

		/* parse the date as a number and return it */
		return String.valueOf(NumberParser.parse(dateComponent));
	}
}
