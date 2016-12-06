/*
 * top-level abstract class for component parsers of a portion of a THING
 *
 */

package com.adtime.common.date.extrator.regexparser.extractor;


import com.adtime.common.date.extrator.regexparser.BindingCallback;
import com.adtime.common.date.extrator.regexparser.Context;

import java.util.Map;
import java.util.logging.Logger;


public abstract class ComponentParser implements BindingCallback {
	/*
	 * the name of the component that this object parses. i.e. 'month-name'
	 * or 'day-numeric'
	 */
	String name;

	Logger log;

	public ComponentParser(Map map) {
		this.name = (String) map.get("type");
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public String getType() {
		return this.name;
	}

	/* return an String corresponding to the portion of the parsed value */
	public abstract String parse(String match);

	/*
	 * the callback that is expected by the main RegexExtractor engine. Takes in a
	 * string match and a ctx, and mutates that context appropriately
	 */
	public boolean processMatch(String match, Object objCtx) {
		Context ctx = (Context) objCtx;

		String val = parse(match);

		ctx.setComponent(name, val);
		
		return true;
	}
}
