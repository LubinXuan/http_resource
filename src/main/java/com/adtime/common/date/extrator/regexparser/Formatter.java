/* 
 * takes as input a string with templated values and generates a
 * printf'ed equivalent. for example, 
 * the pattern 
 *     "%blah%butwhatfun" 
 * with HashMap
 *     blah => "bunny"
 * would produce the string
 *     "bunnybutwhatfun"
 *
 * @version $Id: Formatter.java,v 1.1 2006/09/05 22:47:12 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

import java.util.HashMap;
import java.util.Vector;

public class Formatter
{
  public static final String VAR_BEGIN_MARKER = "<";
  public static final String VAR_END_MARKER = ">";
  public static final char COMMENT_CHAR = '\\';

  private static boolean isCommentedMarker(String input, 
                                           int markerIdx, 
                                           char commentChar)
  {
    return (markerIdx > 0 && input.charAt(markerIdx-1) == commentChar);
  }

  private static int findNextUncommentedMarker(String input,
                                               int startIdx,
                                               String marker,
                                               char commentChar)
  {
    if (startIdx >= input.length())
      return -1;

    int markerIdx = -1;
    while ((markerIdx = input.substring(startIdx).indexOf(marker)) != -1)
    {
      if (!isCommentedMarker(input, startIdx + markerIdx, commentChar))
        // not commented, stop
        break;
      
      /* advance past this match */
      startIdx += markerIdx + 1;
    }
    
    if (markerIdx == -1)
      return markerIdx;
    else
      return markerIdx + startIdx;
  }

  /* returns the substring indeces for the next variable match */
  private static FormatVariableSubstring findNextVariableBinding(String input, 
                                                          int startIdx)
  {
    int varStart = findNextUncommentedMarker(input, startIdx, 
                                             VAR_BEGIN_MARKER, COMMENT_CHAR);
    if (varStart == -1)
      /* the beginning of another marker was not found */
      return null;
    
    int varEnd =  findNextUncommentedMarker(input, varStart+1, 
                                            VAR_END_MARKER, COMMENT_CHAR);

    if (varEnd == -1)
      /* the ending of an opened marker was not found */
      reportError("variable marker found, but was not closed with '" +
                  VAR_END_MARKER + "'",
                  input, varStart);

    int matchBeg = varStart;
    int begIdx = varStart + VAR_BEGIN_MARKER.length();
    int endIdx = varEnd;
    int matchEnd = varEnd + VAR_END_MARKER.length();
    String varName = input.substring(begIdx, endIdx);
    
    return new FormatVariableSubstring(begIdx, endIdx, 
                                       matchBeg, matchEnd, 
                                       varName);
  }
  
  public static String format(String pattern, HashMap variableBindings)
  {
    return format(pattern, variableBindings, new Vector());
  }


  /* return a String with the formatted info inserted according to the
   * bindings. also writes out a vector of binding information for
   * each variable as it was encountered
   */
  public static String format(String pattern, HashMap variableBindings,
                              Vector bindingsFound)
  {
    int matchIdx = 0;
    FormatVariableSubstring fvs = null;
    bindingsFound.clear();
    while ( (fvs = findNextVariableBinding(pattern, matchIdx)) != null)
    {
      bindingsFound.add(fvs);
      /* advance to the end of the current matched variable */
      matchIdx = fvs.matchEnd;
    }

    StringBuffer outputString = new StringBuffer(100);
    int lastMatch = 0;
    int len =  bindingsFound.size();
    for (int i = 0; i < len; i++)
    {
      fvs = (FormatVariableSubstring)bindingsFound.get(i);
      String varName = pattern.substring(fvs.begIdx, fvs.endIdx);
      
      String value = (String)variableBindings.get(varName);
      //System.out.println("found variable " + varName + "=" + value);
      if (value == null)
      {
        String msg = "error, no variable binding found for \"" + varName + "\":";
        reportError(msg, pattern, fvs.begIdx);
      }
      //System.out.println("appending "+ pattern.substring(lastMatch, fvs.matchBeg));
/* add the string in between this and the last match */
      outputString.append(pattern.substring(lastMatch, fvs.matchBeg));

      /* add in the binding for this variable */
      outputString.append(value);
      lastMatch = fvs.matchEnd;
    }

    /* add the portion of the string after the last match */
    outputString.append(pattern.substring(lastMatch));
    
    return outputString.toString();
  }

  private static void reportError(String msg,
                                  String pattern, 
                                  int pos)
  {
    StringBuffer errMsg = new StringBuffer(msg);
    errMsg.append("\n");
    errMsg.append(pattern);
    errMsg.append("\n");
    while (--pos > 0)
      errMsg.append(" ");
    errMsg.append("^");
    System.out.println(errMsg.toString());
    System.exit(-1);
  }

  public static void main(String[] args)
  {
    String pattern = args[0];
    HashMap map = new HashMap();
    for (int i = 1; i < args.length; i++)
    {
      String[] keyAndValue = args[i].split("=");
      map.put(keyAndValue[0], keyAndValue[1]);
    }
    Vector vec = new Vector();
    System.out.println(Formatter.format(pattern, map, vec));
  } 
}
