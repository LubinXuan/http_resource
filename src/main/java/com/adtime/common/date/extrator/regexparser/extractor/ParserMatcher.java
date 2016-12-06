/*
 * matching iterator specifically for date parsing
 *
 * @version $Id: DateParserMatcher.java,v 1.3 2006/10/20 04:20:15 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser.extractor;

import com.adtime.common.date.extrator.regexparser.AnnotatedMatch;
import com.adtime.common.date.extrator.regexparser.ArrayAnnotatedMatcher;
import com.adtime.common.date.extrator.regexparser.Context;

import java.util.HashMap;
import java.util.logging.Logger;


public class ParserMatcher {
	Context dpc;

	ArrayAnnotatedMatcher aam;

	AnnotatedMatch am;

	HashMap bindingCallbacks;

	Logger log;

	public ParserMatcher(ArrayAnnotatedMatcher aam) {
		this.aam = aam;
		this.am = null;
	}

	public Context getContext() {
		return (Context) this.aam.getContext();
	}

	public void setMatchString(String input) {
		this.aam.setMatchString(input);
	}

	public AnnotatedMatch getMatch() {
		return this.am;
	}

	public void setLogger(Logger log) {
		getContext().setLogger(log);
		this.aam.setLogger(log);
		this.log = log;
	}

	public boolean getNextMatch() {
		/* reset the parsing context so that we can get the next date */
		getContext().reset();

		this.am = this.aam.find();
		if (this.am == null) {
			if (this.log != null)
				this.log.info("no matches left");
			/* no matches remain */
			return false;
		}

		if (this.log != null)
			this.log.info("found a match: " + this.am.getBindingMatches());

		return true;
	}
}
