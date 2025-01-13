//=========================================================================
//
//  Part of units package -- a Java version of GNU Units program.
//
//  Units is a program for unit conversion originally written in C
//  by Adrian Mariano (adrian@cam.cornell.edu.).
//  Copyright (C) 1996, 1997, 1999, 2000, 2001, 2002, 2003, 2004,
//  2005, 2006, 2007, 2009, 2011 by Free Software Foundation, Inc.
//
//  Java version Copyright (C) 2003, 2004, 2005, 2006, 2007, 2008,
//  2009, 2011, 2012 by Roman R Redziejowski (www.romanredz.se).
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
//  along with this program. If not, see <http://www.gnu.org/licenses/>.
//
//-------------------------------------------------------------------------
//
//  Change log
//
//  Version 1.84.J07
//   050315 Changed package name to 'units'.
//
//  Version 1.88.J03
//   110623 shownumber: ensured correct decimal rounding.
//          (Java cast from double to float truncated binary
//          representation and produced incorrect rounding.)
//
//  Version 1.88.J04
//   110814 Corrected bug in shownumber: added 'Locale.US' argument
//          in String.format. (Depending on locale, String.format
//          produced decimal comma that was not recognized further on.)
//
//  Version 1.89.J01
//   120202 Removed no longer used method 'strtod' and its 'NumberMatcher'.
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
   * @param  chars characters to look for.
   * @param  s     String to search.
   * @param  start starting index for the search.
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


  //=====================================================================
  //  shownumber
  //=====================================================================
  /**
   * Converts <code>double</code> number to a printable representation.
   *
   * @param  d number to be converted.
   * @return String representation of <code>d</code>.
   */
  public static String shownumber(double d)
    {
      if (Double.isInfinite(d) || Double.isNaN(d))
        return Double.toString(d);

      if (d==(int)d)
        return Integer.toString((int)d);

      StringBuilder s = new StringBuilder(String.format(Locale.US,"%.8G",d));
      int p = s.indexOf("."); // Position of decimal point
      int e = s.indexOf("E"); // Positon of 'E' or -1 if none
      if (e>0)                // If 'E' present remove '+' and leading '0'
      {
        if (s.charAt(e+1)=='+') s.deleteCharAt(e+1);
        if (s.charAt(e+1)=='0') s.deleteCharAt(e+1);
        if (s.charAt(e+1)=='-' && (s.charAt(e+2)=='0')) s.deleteCharAt(e+2);
      }
      else
        e = s.length();

      // e is now position of the end of mantissa + 1.
      // Remove trailing zeros from mantissa.
      while(e>p+1 && s.charAt(e-1)=='0')
      {
        s.deleteCharAt(e-1);
        e--;
      }

      // If mantissa ends now with decimal point, remove the point.
      if (e==p+1) s.deleteCharAt(p);

      return s.toString();
    }
}
