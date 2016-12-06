/* 
 * represents the binding information for the 
 *
 */

package com.adtime.common.date.extrator.regexparser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AnnotatedRegularExpressionBinding
{
  String var;                               /* name of the binding */
  String val; /* a regular expression that this variable points to */
  BindingCallback cbk;                               /* cbk object */
  Logger          log;               /* logger provided to the cbk */

  /* no support for per binding context's yet */
  //  Object          ctx;                     /* ctx given to the cbk */
  
  public AnnotatedRegularExpressionBinding(String varName, 
                                           String varValue)
    throws Exception
  {
    /* no logging */
    this(varName, varValue, null);
  }

  public AnnotatedRegularExpressionBinding(String varName, 
                                           String varValue,
                                           Logger log)
    throws Exception
  {
    /* logging, but no callbacks for this binding */
    this(varName, varValue, null, log);
  }

  public AnnotatedRegularExpressionBinding(String varName, 
                                           String varValue, 
                                           BindingCallback cbk, 
                                           Logger log)
    throws Exception
  {
    this.var = varName;
    this.val = varValue;
    this.cbk = cbk;
    this.log = log;

    /* can only have callbacks for bindings which are "captured" by
     * the regular expression. if nothing is captured, the callback
     * has no captured value to operate on
     */
    if (this.cbk != null && 
        !AnnotatedRegularExpression.isCapturingBinding(this.val))
      throw new Exception("must have a capturing binding if specifying a callback " + this);
  }

  public String getVariableName()
  {
    return this.var;
  }

  public String getVariableValue()
  {
    return this.val;
  }
  
  public BindingCallback getCallback()
  {
    return this.cbk;
  }

  public Logger getLogger()
  {
    return this.log;
  }

  /* convert a list of annotated reg ex bindings to a map */
  public static Map toMap(List bindings)
    throws Exception
  {
    HashMap ret = new HashMap();
    Iterator iter = bindings.iterator();
    while (iter.hasNext())
    {
      AnnotatedRegularExpressionBinding binding = (AnnotatedRegularExpressionBinding)iter.next();
      String var = binding.getVariableName();
      if (ret.get(var) != null)
        throw new Exception("binding " + var + " specified twice");
      else
        ret.put(var, binding);
    } 

    return ret;
  }
}
