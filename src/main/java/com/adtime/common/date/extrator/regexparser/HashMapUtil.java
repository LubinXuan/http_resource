/*
 * utilities for dealing with hash maps
 *
 * @version $Id: HashMapUtil.java,v 1.1 2006/09/05 22:43:56 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

import java.util.HashMap;
import java.util.Iterator;

public class HashMapUtil
{
  public static String dumpHashMap(HashMap map)
  {
    StringBuffer buff = new StringBuffer();
    Iterator iter = map.keySet().iterator();
    while (iter.hasNext())
    {
      Object key = iter.next();
      buff.append(key);
      buff.append(" -> ");
      buff.append(map.get(key));
      buff.append("\n");
    }
    
    return buff.toString();
  }
}
