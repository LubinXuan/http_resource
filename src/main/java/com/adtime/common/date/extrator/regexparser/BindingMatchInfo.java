/*
 * encapsulates the information about an annotated reg ex's binding
 * match
 * 
 * @version $Id: BindingMatchInfo.java,v 1.1 2006/09/27 01:40:12 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

public class BindingMatchInfo
{
  String bindingVariable; /* name of the binding */
  int begMatchIdx;        /* beginning of the match substring */
  int endMatchIdx;        /* end of the match substring */

  public BindingMatchInfo(String bindingVariable, 
                          int begMatchIdx,
                          int endMatchIdx)
  {
    
  }

  
}


