/*
 * Data structure to support annotated regular expression matching.
 * Contains full regex match and a list of the matches for each binding
 *
 * @version $Id: AnnotatedMatch.java,v 1.2 2006/09/27 01:40:12 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

import java.util.HashMap;

public class AnnotatedMatch {
    String fullMatch;
    HashMap<String,String> bindingMatches;
    int fullMatchIndex;
    String prefix;
    String suffix;

    public AnnotatedMatch(String fullMatch, int fullMatchIndex, HashMap<String,String> bindingMatches, String prefix, String suffix) {
        this.fullMatch = fullMatch;
        this.bindingMatches = bindingMatches;
        this.fullMatchIndex = fullMatchIndex;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String getFullMatch() {
        return this.fullMatch;
    }

    public int getFullMatchIndex() {
        return this.fullMatchIndex;
    }

    public String getMatchForBinding(String binding) {
        return bindingMatches.get(binding);
    }

    public HashMap<String,String> getBindingMatches() {
        return this.bindingMatches;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String toString() {
        return "binding matches:\n" + HashMapUtil.dumpHashMap(this.bindingMatches);
    }
}
