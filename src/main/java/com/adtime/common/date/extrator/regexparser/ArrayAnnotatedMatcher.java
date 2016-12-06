/*
 * takes an array of annotated matchers and applies them in order to
 * iterate through all matches in a string
 *
 * @version $Id: ArrayAnnotatedMatcher.java,v 1.5 2006/10/20 20:56:13 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

import com.adtime.common.date.extrator.dateparser.DateParserPreFixHandler;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

public class ArrayAnnotatedMatcher {
    /* all matchers we are using */
    AnnotatedMatcher[] matchers;

    /* map of binding name to AnnotatedRegularExpressionBinding */
    Map bindingMap;

    /*
     * list (we keep this too so that we can determine callback firing orderings
     */
    List bindingList;

    Object context = null;

    CurrentMatchState matchState;

    /* logger for diagnostic information */
    Logger log = null;

    /*
     * composite class to capture the current match state for this matcher
     */
    private static class CurrentMatchState {
        /* which matcher supplied the current match */
        int curMatcherIdx;

        /* top level idx in the match string where our next match starts */
        int curMatchIdx;

        /* the current match */
        AnnotatedMatch curAnnotatedMatch;

        public CurrentMatchState() {
            reset();
        }

        public void reset() {
            this.curMatcherIdx = 0;
            this.curMatchIdx = 0;
            this.curAnnotatedMatch = null;
        }
    }

    public ArrayAnnotatedMatcher(String[] are, /* annotated reg ex's */
                                 List bindings) throws ParseException, Exception {
        this(are, bindings, null);
    }

    public ArrayAnnotatedMatcher(String[] are, /* annotated reg ex's */
                                 List bindings, Logger log) throws ParseException, Exception {
        /*
         * convert the bindings to a map, but save the original list to
		 * guarantee that the callbacks are invoked in the ordering specified
		 */
        this.bindingList = bindings;
        this.bindingMap = AnnotatedRegularExpressionBinding
                .toMap(this.bindingList);
        this.init(toAnnotatedMatcherArray(are, this.bindingList), log);
    }

    private void init(AnnotatedMatcher[] matchers, Logger log) {
        this.matchers = matchers;
        this.setLogger(log);
        this.matchState = new CurrentMatchState();
    }

    public void setLogger(Logger log) {
        this.log = log;
    }

    /* point this matcher against a target string */
    public void setMatchString(String target) {
        if (this.log != null)
            this.log.fine("target is \"" + target + "\"");

        this.matchState.reset();
        for (int i = 0; i < this.matchers.length; i++)
            this.matchers[i].setMatchString(target);
    }

    /* returns false if no match exists */
    public AnnotatedMatch find() {
        /* where do we continue in the string? */
        if (this.matchState.curAnnotatedMatch != null)
			/* skip past the last match */
            this.matchState.curMatchIdx += this.matchState.curAnnotatedMatch.getFullMatch().length();

        int min = Integer.MAX_VALUE;

		/* use the annotated match with the lowest starting index in the string */
        this.matchState.curAnnotatedMatch = null;

        Vector<AnnotatedMatch> ties = new Vector<AnnotatedMatch>();
        for (int i = 0; i < this.matchers.length; i++) {
            AnnotatedMatch am = this.matchers[i].find(this.matchState.curMatchIdx);
            if (this.log != null)
                this.log.info("matcher "
                        + i
                        + ": got match: "
                        + am
                        + ((am != null) ? ", with match index "
                        + am.getFullMatchIndex() : ""));
            if (am != null) {
                int matchIdx = am.getFullMatchIndex();
                if (matchIdx < min) {
                    min = matchIdx;
                    this.matchState.curMatcherIdx = i;
                    this.matchState.curAnnotatedMatch = am;
                    ties.clear();
                } else if (matchIdx == min) {
					/* a tie was seen! */
                    ties.add(am);
                }
            }
        }

        if (this.log != null)
            this.log.info("matcher " + this.matchState.curMatcherIdx
                    + " chosen" + " in front of " + ties.size() + " ties");

		/* reset the match idx to point to the start of the next match */
        this.matchState.curMatchIdx = min;

		/*
		 * after getting a match, if there are any callbacks defined, fire them
		 * off now
		 */
        fireBindingCallbacks(this.matchState.curAnnotatedMatch);

		/* return true if we found a match */
        return this.matchState.curAnnotatedMatch;
    }

    private boolean fireBindingCallbacks(AnnotatedMatch am) {
        if (am == null)
			/* no match available */
            return true;

        for (String bindingName : am.getBindingMatches().keySet()) {
            String bindingMatch = am.getMatchForBinding(bindingName);
            AnnotatedRegularExpressionBinding binding = (AnnotatedRegularExpressionBinding) this.bindingMap.get(bindingName);
            BindingCallback cbk = binding.getCallback();

            if (cbk != null) {
                if (this.log != null)
                    this.log.info("firing callback " + cbk.getClass().getName()
                            + " for binding " + bindingName + " and match "
                            + bindingMatch);

                cbk.processMatch(bindingMatch, this.context);
            }
        }
        DateParserPreFixHandler.handle(am.prefix,this.context);
        return true;
    }

    /* which matcher was used for this match ? */
    public int getMatcherIdx() {
        return this.matchState.curMatcherIdx;
    }

    /* which index of the target string does this match start on? */
    public int getMatchIdx() {
        return this.matchState.curMatchIdx;
    }

    public Object getContext() {
        return this.context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public List getBindingList() {
        return this.bindingList;
    }

    public void setContext(String contextClass, Map contextArgs)
            throws Exception {
		/* instantiate the context if it has been specified */
        if (contextClass != null && !contextClass.trim().equals(""))
            this.context = instantiateClass(contextClass, contextArgs, null,
                    null);
    }

    private static AnnotatedMatcher[] toAnnotatedMatcherArray(String[] regExs,
                                                              List bindings) throws ParseException {
        AnnotatedMatcher[] ams = new AnnotatedMatcher[regExs.length];
        for (int i = 0; i < regExs.length; i++)
            ams[i] = AnnotatedRegularExpression.compile(regExs[i], bindings);

        return ams;
    }

    public static Object instantiateClass(String className, Map args,
                                          Class extendClass, Class[] implementedClasses) {
        if (className == null || className.trim().equals(""))
            return null;

        try {
            Object[] objs = new Object[1];
            objs[0] = args;

            Class[] signature = {Map.class};
            Object newObject = Class.forName(className).getConstructor(
                    signature).newInstance(objs);

            if (extendClass != null && !extendClass.isInstance(newObject))
                throw new Exception("class " + className
                        + "does not (in)directly extend"
                        + extendClass.getName());
            else if (implementedClasses != null) {
                for (int i = 0; i < implementedClasses.length; i++) {
                    if (!implementedClasses[i].isInstance(newObject))
                        throw new Exception("class " + className
                                + "does not (in)directly implement "
                                + implementedClasses[i].getName());
                }
            }

			/* all checks passed, return the object */
            return newObject;
        } catch (Exception e) {
            System.out.println("error while loading class " + className);
            e.printStackTrace(System.out);

            System.err.println("error while loading class " + className);
            e.printStackTrace(System.err);

            System.exit(-1);
        }

        return null;
    }

    /* only supports single level JSON objects for now */
    public static Map toMap(JSONObject obj) {
        if (obj == null)
            return null;

        HashMap ret = new HashMap();
        for (String s : obj.keySet()) {
            Object val = obj.get(s);
            if (null != val) {
                if (val instanceof JSONObject) {
                    ret.put(s, toMap((JSONObject) val));
                } else if (val instanceof JSONArray) {
                    ret.put(s, toList((JSONArray) val));
                } else {
                    ret.put(s, String.valueOf(val));
                }
            } else {
                ret.put(s, val);
            }
        }

        return ret;
    }

    public static List toList(JSONArray arr) {
        if (arr == null)
            return null;

        List ret = new LinkedList();
        int len = arr.size();
        for (int i = 0; i < len; i++) {
            Object val = arr.get(i);
            if (null != val) {
                if (val instanceof JSONObject) {
                    ret.add(toList((JSONArray) val));
                } else if (val instanceof JSONArray) {
                    ret.add(toMap((JSONObject) val));
                } else {
                    ret.add(String.valueOf(val));
                }
            } else {
                ret.add(val);
            }
        }

        return ret;
    }

}
