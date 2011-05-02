//=========================================================================
//
//  Part of units package -- a Java version of GNU Units program.
//
//  Units is a program for unit conversion originally written in C
//  by Adrian Mariano (adrian@cam.cornell.edu.).
//  Copyright (C) 1996, 1997, 1999, 2000, 2001, 2002, 2003, 2004,
//  2005, 2006, 2007 by Free Software Foundation, Inc.
//
//  Java version Copyright (C) 2003, 2004, 2005, 2006, 2007, 2008,
//  2009 by Roman R Redziejowski (roman.redz@tele2.se).
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//-------------------------------------------------------------------------
//
//  Change log
//
//    050315 Version 1.84.J07. Changed package name to "units".
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;




//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class Util
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 * Holds common methods used by other classes.
 */

 public class Util
{
  //=====================================================================
  //  indexOf
  //=====================================================================
  /**
   * Finds occurrence of one of given characters in a string.
   * <br>
   * Finds the first occurrence in String <code>s</code>,
   * at or after index <code>start</code>,
   * of any of the characters contained in <code>chars</code>.
   *
   * @param  s     String to search.
   * @param  start starting index for the search.
   * @param  chars characters to look for.
   * @return       index of the found occurrence,
   *               or length of <code>s</code> if none found.
   */
  public static int indexOf(final String chars, final String s, int start)
    {
      for (int i=start;i<s.length();i++)
        if (chars.indexOf(s.charAt(i))>=0)
          return i;
      return s.length();
    }

  
  private static NumberFormat df = NumberFormat.getInstance(Locale.US);
  private static NumberFormat df_exp = NumberFormat.getInstance(Locale.US);
  
  static {
	  if (df instanceof DecimalFormat){
		  ((DecimalFormat) df).applyPattern("#.############");
		  ((DecimalFormat) df_exp).applyPattern("#.############E0");
	  }
  }

  //=====================================================================
  //  shownumber
  //=====================================================================
  /**
   * Converts <code>double</code> number to a printable representation.
   *
   * @param  d number to be converted.
   * @return   String representation of <code>d</code>.
   */
  public static String shownumber(double d)
    {
	  if ((d > 0 && d < 1E-3) 
		|| (d < 0 && d > -1E-3) 
		|| d > 1E6 
		|| d < -1E6){
		return df_exp.format(d);  
	  }else{
		  return df.format(d);
	  }
    }

  //=====================================================================
  //  strtod
  //=====================================================================
  /**
   * Emulates (part of) C/C++ library function <code>strtod</code>.
   * <br>
   * Finds the longest substring of <code>s</code> starting
   * at index <code>i</code> that represents a <code>double</code> number
   * in C/C++ format.
   *
   *  @param  s String to be scanned.
   *  @param  i starting index for the scan.
   *  @return   index of the last recognized character plus 1,
   *            or <code>i</code> if nothing was recognized.
   */
  public static int strtod (final String s, int i)
    {
      NumberMatcher nm = new NumberMatcher(s,i);
      return nm.match();
    }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  class NumberMatcher
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  /**
   * Recognizer of numbers in C/C++ <code>double</code> format.
   * <br>
   * Auxiliary class used in the method <code>strtod</code>
   * to match regular expression
   *
   * <pre>[ \t]*[+-]?((([0-9]+(.[0-9]*)?)|(.?[0-9]+))([Ee][+-]?[0-9]+)?)[ \t]*</pre>
   *
   * Can be replaced by classes from package java.util.regex
   * on Java version 1.4 or later.
   */

  private static class NumberMatcher
  {
    private String s;
    private int i;    // matched so far
    private int j;    // currently look at
    private int c;    // character at j

    //===================================================================
    //  Construct NumberMatcher to work on string 'str'
    //  starting at position 'start'.
    //===================================================================
    NumberMatcher(final String str, int start)
      {
        s = str;
        i = start;
        j = i-1;
        getNext();
      }

    //===================================================================
    //  Advance to next character; simulate '\n' at the end of string.
    //===================================================================
    private void getNext()
      {
        j++;
        c = j>=s.length()? '\n' : s.charAt(j);
      }

    //===================================================================
    //  Match 'n'; return false if not matched.
    //===================================================================
    private boolean symbol(int n)
      {
        if (c!=n) return false;
        getNext();
        return true;
      }

    //===================================================================
    //  Match any character from 's'; return false if not matched.
    //===================================================================
    private boolean oneOf(String s)
      {
        if (s.indexOf(c)<0) return false;
        getNext();
        return true;
      }

    //===================================================================
    //  Match [0-9]; return false if not matched.
    //===================================================================
    private boolean digit()
      { return oneOf("0123456789"); }

    //===================================================================
    //  Match [0-9]+(.[0-9]*)?; return false if not matched.
    //===================================================================
    private boolean mantissa1()
      {
        if (!digit()) return false;
        while(digit());
        if (!symbol('.')) return true;
        while(digit());
        return true;
      }

    //===================================================================
    //  Match .[0-9]+; return false if not matched.
    //===================================================================
    private boolean mantissa2()
      {
        if (!symbol('.')) return false;
        if (!digit()) return false;
        while(digit());
        return true;
      }

    //===================================================================
    //  Match [Ee][+-]?[0-9]+)?; return false if not matched.
    //===================================================================
    private boolean exponent()
      {
        if (!oneOf("eE")) return true;
        oneOf("+-");
        if (!digit()) return false;
        while(digit());
        return true;
      }

    //===================================================================
    //  Match number.
    //===================================================================
    int match()
      {
        while(oneOf(" \t"));
        oneOf("+-");
        if (!mantissa1()&&!mantissa2()) return i;
        i = j;
        if (!exponent()) return i;
        i = j;
        while(oneOf(" \t"));
        return j;
      }
  }

}
