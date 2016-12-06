/*
 * matching iterator specifically for date parsing
 *
 * @version $Id: DateParserMatcher.java,v 1.3 2006/10/20 04:20:15 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.dateparser;

import com.adtime.common.date.extrator.regexparser.AnnotatedMatch;
import com.adtime.common.date.extrator.regexparser.ArrayAnnotatedMatcher;

import java.util.HashMap;
import java.util.logging.Logger;


public class DateParserMatcher {
    DateParsingContext dpc;
    ArrayAnnotatedMatcher aam;
    AnnotatedMatch am;
    HashMap bindingCallbacks;
    Logger log;

    public DateParserMatcher(ArrayAnnotatedMatcher aam) {
        this.aam = aam;
        this.am = null;
    }

    public DateParsingContext getDateParsingContext() {
        return (DateParsingContext) this.aam.getContext();
    }

    public DateParsingContext getDateParsingContextFillDefault() {
        DateParsingContext context = (DateParsingContext) this.aam.getContext();
        if (null != context) {
            return context.fillInDefaults();
        } else {
            return null;
        }
    }

    public void setMatchString(String input) {
        this.aam.setMatchString(input);
    }

    public AnnotatedMatch getMatch() {
        return this.am;
    }

    public void setLogger(Logger log) {
        getDateParsingContext().setLogger(log);
        this.aam.setLogger(log);
        this.log = log;
    }

    public boolean getNextDate() {
    /* reset the parsing context so that we can get the next date */
        getDateParsingContext().reset();

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
