/*
 * implements function for annotated regular expressions. for example,
 *
 * regex: "%boo%?somename"
 * with binding: boo -> foo
 *
 * when matching this regular expression to the input string
 * "foosomename" we would get the data structure
 *
 * full match: foosomename
 * boo: foo
 *
 * likewise, for the input string "barsomename" we would get:
 *
 * full match: somename
 * boo: ""
 * 
 * @version $Id: AnnotatedRegularExpression.java,v 1.6 2006/10/20 20:54:19 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;


public class AnnotatedRegularExpression {
    /* composite class for parsing metadata */
    private static class ParseMetadata {
        /* non capturing indicator for a binding */
        public static final String NON_CAPTURE_PREFIX = "?:";

        boolean capturing;
        /* changed to look like the finalized form of a binding value
         * i.e. all capturing gruops stripped off, and one broad based
         * capturing group tacked on
         */
        String normalizedBinding;

        static boolean isCapturingBinding(String bindingValue) {
            return !bindingValue.startsWith(NON_CAPTURE_PREFIX);
        }

        static ParseMetadata parse(String bindingValue) {
            ParseMetadata ret = new ParseMetadata();
            ret.capturing = isCapturingBinding(bindingValue);
            ret.normalizedBinding = "(" + removeAllCapturingGroups(bindingValue) + ")";
            return ret;
        }

        static String removeAllCapturingGroups(String annotatedRegEx) {
      /* find all capturing groups and make them non capturing */
            StringBuilder buff = new StringBuilder(annotatedRegEx.length());
            int curIdx = 0;
            int nxtIdx;
            while ((nxtIdx = annotatedRegEx.indexOf('(', curIdx)) != -1) {
        /* determine if this is a commented group or not */
                boolean isGroup = true;
                if (nxtIdx > 0) {
                    char prevChar = annotatedRegEx.charAt(nxtIdx - 1);
                    if (prevChar == '\\')
                        isGroup = false;
                }
        
        /* grab and copy the string between curIdx, nxtIdx and add it to
         * the buffer 
         */
                String stringPart = annotatedRegEx.substring(curIdx, nxtIdx + 1);
                buff.append(stringPart);
                if (isGroup)
                    buff.append("?:");
    
        /* go to the next part of this string */
                curIdx = nxtIdx + 1;
            }
    
      /* fill in the last part of the string */
            buff.append(annotatedRegEx.substring(curIdx));

            return buff.toString();
        }
    }

    /* logger for AnnotatedRegularExpression library */
    public static Logger log = null;

    public static boolean isCapturingBinding(String bindingValue) {
        return ParseMetadata.isCapturingBinding(bindingValue);
    }

    public static void setLogger(Logger logger) {
        log = logger;
    }

    public static AnnotatedMatcher compile(String expression,
                                           List origBindings)
            throws ParseException {
    /* construct a list of modified bindings */
        HashMap<String,String> parsedBindings = new HashMap<String,String>();
        HashMap<String,ParseMetadata> parseMetadataInfo = new HashMap<String,ParseMetadata>();

    /* go through the list of bindings and get their parse info */
        for (Object origBinding : origBindings) {
            AnnotatedRegularExpressionBinding areb = (AnnotatedRegularExpressionBinding) origBinding;
            String varName = areb.getVariableName();
            String varValue = areb.getVariableValue();

            ParseMetadata pm = ParseMetadata.parse(varValue);
            parseMetadataInfo.put(varName, pm);
            parsedBindings.put(varName, pm.normalizedBinding);
        }

    /* construct the finalized regular expression */
        Vector variableList = new Vector();
        String expressionWithoutCapturingGroups = ParseMetadata.removeAllCapturingGroups(expression);
        String fullRegExp = Formatter.format(expressionWithoutCapturingGroups,
                parsedBindings,
                variableList);

    /* use the list to construct the grouping offsets for this
     * annotated reg ex. i.e. the mapping from each variable to its
     * group in the full reg ex 
     */

    /* maps from variable names to their group number in the regular
     * expression 
     */
        HashMap<String, Integer> groupingOffsets = new HashMap<String, Integer>();
        int curGroupOffset = 1;
        for (Object aVariableList : variableList) {
            FormatVariableSubstring fvs = ((FormatVariableSubstring) aVariableList);
            String varName = fvs.getVarName();
            ParseMetadata pm = parseMetadataInfo.get(varName);

            if (pm.capturing) {
        /* before adding the group offset, check to see if there is
         * already an offset for this group. if so, raise an error 
         */
                Integer groupOffset = groupingOffsets.get(varName);
                if (groupOffset != null)
                    throw new ParseException("error, got two occurances of the variable '" +
                            varName + "' in expression " + expression,
                            fvs.getBeginIdx());
                else {
          /* add in the offset */
                    groupingOffsets.put(varName, curGroupOffset);
                    curGroupOffset++;
                }
            }
        }

        if (log != null) {
            log.info("full regex is " + fullRegExp);
            log.info("offsets:");
            log.info(HashMapUtil.dumpHashMap(groupingOffsets));
        }

        return new AnnotatedMatcher(fullRegExp, groupingOffsets);
    }

    public static void main(String[] args) throws Exception {
        String pattern = RegularExpressionUtil.unescapeUnicodeCharacters(args[0]);
        String testData = RegularExpressionUtil.unescapeUnicodeCharacters(args[1]);
        LinkedList<AnnotatedRegularExpressionBinding> list = new LinkedList<AnnotatedRegularExpressionBinding>();
        for (int i = 2; i < args.length; i++) {
            String[] keyAndValue = args[i].split("=");
            String varName = keyAndValue[0];
            String varValue = RegularExpressionUtil.unescapeUnicodeCharacters(keyAndValue[1]);
            list.add(new AnnotatedRegularExpressionBinding(varName, varValue));
        }

        AnnotatedMatcher matcher = AnnotatedRegularExpression.compile(pattern,
                list);
        matcher.setMatchString(testData);

        int count = 0;
        AnnotatedMatch match;
        while ((match = matcher.find()) != null)
            System.out.println("match " + count++ + ": " + match);

    }
}

