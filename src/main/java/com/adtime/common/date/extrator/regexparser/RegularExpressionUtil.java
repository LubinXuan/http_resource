/* 
 * testing utility for running regular expressions from the command line
 *
 * @version $Id: RegularExpressionUtil.java,v 1.2 2006/10/18 03:37:57 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpressionUtil
{
  /* converts characters that have strings in them to unicode escaped
   * characters
   */
  public static final int MAX_ASCII_VALUE = 127;
  public static String escapeNonAsciiCharacters(String data)
  {
    StringBuffer buff = new StringBuffer();
    int curIdx = 0;
    int len = data.length();
    while (curIdx < len)
    {
      char curChar = data.charAt(curIdx);
      int charVal = (int)curChar;
      if (charVal < MAX_ASCII_VALUE)
        /* valid ascii, accept it */
        buff.append(curChar);
      else
      {
        /* non ascii character create a string representation
         * of an escaped unicode character
         */
        String hexString = Integer.toString(charVal, 16);
        
        buff.append("\\u");

        /* left pad with 0's until this has 4 characters  */
        int padding = 4 - hexString.length();
        while (padding-- > 0)
          buff.append("0");

        buff.append(hexString);
      }

      curIdx++;
    } 

    return buff.toString();
  }
  
  /* converts strings with unicode escapes to contain unicode
   * characters, i.e. the literal string "\u0020hi" is converted to 
   * " hi". 
   */
  public static String unescapeUnicodeCharacters(String data)
  {
    StringBuffer buff = new StringBuffer();
    int curIdx = 0;
    int prvIdx = 0;

    while ( (curIdx = data.indexOf("\\u", curIdx)) != -1)
    {
      /* append the characters we skipped over */
      buff.append(data.substring(prvIdx, curIdx));

      if (curIdx == 0 || data.charAt(curIdx-1) != '\\')
      {
        /* real unicode escape character, parse the next 4 chars as hex */
        int charVal = Integer.parseInt(data.substring(curIdx+2, curIdx+2+4), 16);
        char character = (char)charVal;
        buff.append(character);
        curIdx += 6;
      }
      else
      {
        /* escaped unicode character, skip it! */
        buff.append("\\u");
        curIdx += 2;
      }
                  
      prvIdx = curIdx;
    }
    
    /* append last part of string */
    buff.append(data.substring(prvIdx));
    return buff.toString();

  }

  public static String makeCharacters(int count, char character)
  {
    StringBuffer buff = new StringBuffer();
    while (count-- > 0)
      buff.append(character);
    return buff.toString();
  }

  public static void main(String[] args)
  {
    if (1==1)
    {
      String data = unescapeUnicodeCharacters(args[0]);
      System.out.println(escapeNonAsciiCharacters(data));
      return;
    }

    String pattern = args[0];
    String data = unescapeUnicodeCharacters(args[1]);

    Pattern pat = Pattern.compile(pattern);
    Matcher matcher = pat.matcher(data);
    
    int matchCount = 0;
    while(matcher.find())
    {
      System.out.println("--------------------\nmatch " + matchCount++ + ": ");
      System.out.println(data);
      System.out.println(makeCharacters(matcher.start(0), ' ') +
                         makeCharacters(matcher.end(0) - matcher.start(0), '^'));
      for (int i = 0; i < matcher.groupCount(); i++)
        System.out.println("group " + i + ": " + matcher.group(i+1));
    }
  }
}
