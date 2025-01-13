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
//  2009, 2012 by Roman R Redziejowski (www.romanredz.se).
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
//  Version 1.89.J01.
//    120208 Created.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import static net.sourceforge.unitsinjava.UnitList.RoundMode.FLOATING_POINT;
import static net.sourceforge.unitsinjava.UnitList.RoundMode.INTEGER;
import static net.sourceforge.unitsinjava.UnitList.RoundMode.SEPARATED_FRACTION;

import java.util.Vector;
import java.util.Locale;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class UnitList
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Represents a semicolon-separated list of unit expressions.
 *  <br>
 *  Contains methods for evaluating and checking the expression
 *  and conversion of given Value to sum of units.
 */
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

 public class UnitList
{
  //-------------------------------------------------------------------
  /** Number of unit expressions. */
  //-------------------------------------------------------------------
  public int n;

  //-------------------------------------------------------------------
  /** The expressions, trimmed. */
  //-------------------------------------------------------------------
  public String[] unit;

  //-------------------------------------------------------------------
  /** Evaluated and reduced expressions, checked for compatibility. */
  //-------------------------------------------------------------------
  public Value[] value;

  public double[] result;

  //-------------------------------------------------------------------
  /** Indicates handling of the last expression:
   *  <br> 0 = round to integer;
   *  <br> 1 = show as integer.fraction;
   *  <br> 2 = split into integer and fraction. */
  //-------------------------------------------------------------------
  public enum RoundMode { INTEGER, FLOATING_POINT, SEPARATED_FRACTION }
  public RoundMode roundMode = FLOATING_POINT;

  public double rounded;
  public double roundAmount;


  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs a UnitList object from a given string.
   *  Evaluates and reduces the expressions.
   *  Throws EvalError if expressions cannot be evaluated
   *  or are not compatible.
   *
   *  @param unitlist a semicolon-separated list of unit expressions.
   */
  public UnitList(final String unitlist)
    {
      //---------------------------------------------------------------
      //  Decide handling of the last element.
      //---------------------------------------------------------------
      String list = unitlist.trim();
      int lg = list.length();

      if (list.endsWith(";;"))
      {
        roundMode = INTEGER;
        list = list.substring(0,lg-2);
        lg -= 2;
      }

      else if (list.endsWith(";"))
      {
        roundMode = SEPARATED_FRACTION;
        list = list.substring(0,lg-1);
        lg --;
      }

      if (Env.round) roundMode = INTEGER; // Option '-r' overrides ';'


      //---------------------------------------------------------------
      //  Split string into expressions.
      //---------------------------------------------------------------
      Vector<String> items = new Vector<>();
      int i = 0;
      while(i<=lg)
      {
        int j = list.indexOf(';',i);
        if (j<0) j = lg;
        items.add(list.substring(i,j).trim());
        i = j+1;
      }

      n = items.size();

      unit = new String[n];
      items.toArray(unit);

      //---------------------------------------------------------------
      //  Evaluate the expressions.
      //---------------------------------------------------------------
      value = new Value[n];

      for (i=0;i<n;i++)
      {
        if (unit[i].length() == 0)
           throw new EvalError("Element " + (i+1) + " is empty.");

        value[i] = null;
        try
          { value[i] = Value.parse(unit[i]); }
        catch (EvalError e)
          { throw new EvalError("Element " + (i+1) +
                   " is not valid. " + e.getMessage()); }
        value[i].completereduce();
      }

      //---------------------------------------------------------------
      // Check compatibility.
      //---------------------------------------------------------------
      for (i=1;i<n;i++)
      {
        if (!value[i-1].isCompatibleWith(value[i],Ignore.DIMLESS))
          throw new EvalError("Conformability error:" +
                 "\n\t" + unit[i-1] + " = " + value[i-1].asString() +
                 "\n\t" + unit[i] + " = " + value[i].asString());
      }
    }

  //=====================================================================
  //  isUnitList
  //=====================================================================
  /**
   *  Checks if given string represents a unit list.
   *  The string may be either a unit list or an alias for it.
   *
   *  $param s the string to be checked.
   *  @return a unit list or null.
   */
  public static String isUnitList(final String s)
    {
      Alias alias = Alias.table.get(s);
      if (alias!=null)
        return alias.unitList;

      if (s.indexOf(';')>0)
        return s;

      return null;
    }


  //=====================================================================
  //  convert
  //=====================================================================
  /**
   *  Prints result of conversion of unit expression to sum of units
   *  as specified by this list. The list must be evaluated and checked.
   *
   *  @param  fromExpr  expression to be converted.
   *  @param  fromValue evaluated 'from' expression, completely reduced.
   *  @return <code>true</code> if conversion was successful,
   *          <code>false</code> otherwise.
   */
  public boolean convert(final String fromExpr, final Value fromValue)
    {
      //---------------------------------------------------------------
      //  Check compatibility with the list.
      //---------------------------------------------------------------
      if (!fromValue.isCompatibleWith(value[0],Ignore.DIMLESS))
      {
        Env.out.println("Conformability error");
        Env.out.println("\t" + fromExpr + " = " + fromValue.asString());
        Env.out.println("\t" + unit[0] + " = " + value[0].asString());
        return false;
      }

      //--------------------------------------------------------------
      //  Perform the conversion.
      //  No rounding yet, as it should be done on converted value.
      //---------------------------------------------------------------
      final double DOUBLE_ACCURACY = 1e-16;

      result = new double[n];
      double rem = fromValue.factor;
      double fromAccuracy = DOUBLE_ACCURACY * fromValue.factor;
      for (int i = 0; i < n-1; i++)
      {
        result[i] = Math.floor(rem / value[i].factor);  // Integer quotient
        rem = rem - result[i] * value[i].factor;        // Remainder
        double floatAccuracy = fromAccuracy / value[i].factor;
        if (rem < floatAccuracy) {
          rem = 0;
          break;
        }
      }

      result[n-1] = rem / value[n-1].factor;

      roundAmount = 0; // Positive if rounded up

      //---------------------------------------------------------------
      //  Round the lowest value.
      //---------------------------------------------------------------
      if (roundMode==INTEGER) // If requested, round to integer.
      {
        rounded = Math.floor(result[n-1]+0.5);
        roundAmount = rounded - result[n-1];
      }

      else          // Otherwise round to displayed precision
        rounded = Double.parseDouble(String.format(Locale.US, "%.8G", result[n - 1]));

      result[n-1] = rounded;

      //---------------------------------------------------------------
      //  If requested, split lowest value into integer and fraction.
      //---------------------------------------------------------------
      if (roundMode==SEPARATED_FRACTION)
      {
        result[n-1] = Math.floor(rounded);
        rounded = rounded - result[n-1];
      }

      //---------------------------------------------------------------
      //  Final rounding.
      //  Going backwards, we propagate 1 if the value
      //  is identical to next higher unit.
      //---------------------------------------------------------------
      for (int i=n-1;i>0;i--)
      {
        double nextUnit = value[i-1].factor / value[i].factor;
        nextUnit = Double.parseDouble(String.format(Locale.US,"%.8G",nextUnit));
        if (result[i]==nextUnit)
        {
          result[i] = 0;
          result[i-1] += 1;
        }
      }

      //---------------------------------------------------------------
      //  Print the result.
      //---------------------------------------------------------------
      StringBuilder sb = new StringBuilder();

      if (Env.verbose>0)
      {
        //-------------------------------------------------------------
        //  Standard or verbose
        //-------------------------------------------------------------
        sb.append("\t");

        if (Env.verbose==2) sb.append(fromExpr + " = ");

        String sep = "";
        for (int i=0;i<n;i++)
          if (result[i]!=0)
          {
            sb.append(sep + showUnit(result[i],unit[i]));
            sep = " + ";
          }
        if (roundMode==SEPARATED_FRACTION)
          if (rounded!=0)
            sb.append(sep + showUnit(rounded,unit[n-1]));

        if (sb.length()==0)
          sb.append("0 " + unit[0]);

        if (roundAmount>0)
          sb.append(" (rounded up to nearest " + unit[n-1] + ")");
        if (roundAmount<0)
          sb.append(" (rounded down to nearest " + unit[n-1] + ")");
      }

      else
      {
        //-------------------------------------------------------------
        //  Terse
        //-------------------------------------------------------------
        String sep = "";
        for (int i=0;i<n;i++)
        {
          sb.append(sep + Util.shownumber(result[i]));
          sep = ";";
        }

        if (roundMode==SEPARATED_FRACTION)
          sb.append(sep + Util.shownumber(rounded));

        if (roundAmount>0)
          sb.append(";-");
        if (roundAmount<0)
          sb.append(";+");
      }

      Env.out.println(sb.toString());
      return true;
    }


  //=====================================================================
  //  showUnit
  //=====================================================================
  /**
   *  Constructs representation of result value and the expression
   *  received in unit list, to be shown as part of unit sum.
   *  (A free interpretation of the original 'showunitname'.)
   *
   *  @param value a value resulting from the conversion.
   *  @param unit the corresponding expression from the unit list.
   *  @return the constructed representation.
   */
  public static String showUnit(double value, final String unit)
    {
      //----------------------------------------------------------------
      //  The processing depends on the form of 'unit'.
      //  Case: 'unit' is an expression containing arithmetic operators.
      //----------------------------------------------------------------
      int p = Util.indexOf("+-",unit,0);
      if (p!=unit.length())
      {
        if (value==1)
          return "(" + unit + ")";
        else
          return Util.shownumber(value) + " (" + unit + ")";
      }

      //----------------------------------------------------------------
      //  Case: 'unit' begins with '1|integer', where integer<10000,
      //  and 'value' is an integer<10000, replace 1 by 'value'.
      //----------------------------------------------------------------
      if (value==(int)value
          && value<10000
          && unit.length()>2
          && unit.startsWith("1|")
          )
      {
        int i=2;
        while(i<unit.length() && "0123456789".indexOf(unit.charAt(i))>=0)
          i++;

        if (i>1 && i<6)
          return Util.shownumber(value) + unit.substring(1);
      }


      //----------------------------------------------------------------
      //  Case: 'unit' begins with a number.
      //----------------------------------------------------------------
      if ("0123456789.,".indexOf(unit.charAt(0))>=0)
      {
        if (value==1)
          return unit;
        else
          return Util.shownumber(value) + " * " + unit;
      }

      //----------------------------------------------------------------
      //  Case: other.
      //----------------------------------------------------------------
      return Util.shownumber(value) + " " + unit;
    }

}

