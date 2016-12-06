/* 
 * used to reflect a substring portion of a formatted
 * string that matches a variable name 
 *
 * @version $Id: FormatVariableSubstring.java,v 1.1 2006/09/05 22:47:11 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

public class FormatVariableSubstring
{
  int begIdx = -1; /* idx of the beg/end of the variable name */
  int endIdx = -1;
  int matchBeg = -1; /* idx of beg/end of the entire variable     */
  int matchEnd = -1; /* substring, including the variable markers */
  String varName = null;
  
  public FormatVariableSubstring(int begIdx, int endIdx, 
                                 int matchBeg, int matchEnd, 
                                 String varName)
  {
    this.begIdx = begIdx;
    this.endIdx = endIdx;
    this.matchBeg = matchBeg;
    this.matchEnd = matchEnd;
    this.varName = varName;
  }
  
  public String getVarName()
  {
    return this.varName;
  }

  public int getBeginIdx()
  {
    return this.begIdx;
  }

  public int getEndIdx()
  {
    return this.endIdx;
  }

  public int getMatchBeginIdx()
  {
    return this.matchBeg;
  }

  public int getMatchEndIdx()
  {
    return this.matchEnd;
  }
}
  
