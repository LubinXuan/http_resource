/*
 * interface that all binding callbacks should support
 *
 * @version $Id: BindingCallback.java,v 1.2 2006/10/20 04:17:28 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser;

public interface BindingCallback
{
  /* note that all binding callbacks must be instantiable from a
   * constructor that takes a Map. unfortunately, this requirement
   * can't be expressed via an interface
   */
  
  /* called when a match has been found. arguments include the string
   * that was matched for this binding, and the context being used for
   * this array annotated regular expression 
   */
  public boolean processMatch(String match, Object ctx);
}
