/*
 * mimics the matcher class in java.regex for annotated matches
 *
 * @version $Id: AnnotatedMatcher.java,v 1.5 2006/10/20 20:52:45 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotatedMatcher {
    /* string to which the regular expression will be applied */
    String matchString;

    /* regular expression patter and matcher */
    Pattern pattern;
    Matcher matcher;

    /* current index into the match */
    int startIndex;

    /* maps from variable names to their group number in the regular
     * expression
     */
    HashMap<String, Integer> groupingOffsets;

    public AnnotatedMatcher(String regExpression, HashMap<String, Integer> groupingOffsets) {
        this.pattern = Pattern.compile(regExpression);
        this.groupingOffsets = groupingOffsets;
    }

    public void setMatchString(String matchString) {
        this.matchString = matchString;
        this.matcher = this.pattern.matcher(matchString);
    /* default starting index of 0 */
        this.startIndex = 0;
    }

    public AnnotatedMatch find() {
        return find(this.startIndex);
    }

    public AnnotatedMatch find(int offset) {
    /* if we've reached the end of the string, return null */
        if (offset >= this.matchString.length() || offset < 0)
            return null;

    /* use the grouping offsets to determine how each reg ex matched */
        if (this.matcher.find(offset)) {
      /* iterate through each variable and assign it to the group
       * value as necessary 
       */
            String fullMatchStr = this.matcher.group(0);
            HashMap<String, String> varMatches = new HashMap<String, String>();
            for (String varName : this.groupingOffsets.keySet()) {
        /* find the group offset and assign it's match */
                int groupOffset = this.groupingOffsets.get(varName);
                varMatches.put(varName, this.matcher.group(groupOffset));
            }

            this.startIndex = this.matcher.end();
            String prefix = null, suffix = null;
            if (null != fullMatchStr && fullMatchStr.trim().length() > 0) {
                int preIdx = this.matchString.indexOf(fullMatchStr);
                if (preIdx > 0) {
                    prefix = this.matchString.substring(0, preIdx).trim();
                }
                if (this.startIndex > 0) {
                    suffix = this.matchString.substring(this.startIndex).trim();
                }
            } else {
                fullMatchStr = "";
            }
      /* construct an annotated match from the matcher object and return it */
            return new AnnotatedMatch(fullMatchStr, this.matcher.start(), varMatches, prefix, suffix);
        } else
      /* no matches remaining, get out */
            return null;
    }
}
