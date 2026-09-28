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
//  along with this program. If not, see <http://www.gnu.org/licenses/>.
//
//-------------------------------------------------------------------------
//
//  Change log
//
//    050315 Version 1.84.J07. Changed package name to "units".
//    061229 Version 1.86.J01. Corrected test for 'verbose'.
//    070102 Suppress printing "\tDefinition :" and tabs for compact output.
//    091024 Version 1.87.J01.
//           Used modified 'insertAlph'.
//           Used generics for 'x'.'y'.
//    091025 Replaced 'Parser.Exception' by 'EvalError'.
//    091031 Moved definition of Ignore to Factor.
//           Replaced 'addtolist' by 'isCompatibleWith'.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Hashtable;
import java.util.Vector;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class TabularFunction
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A function defined by a table (a 'piecewise linear unit'.)
 */

 public class TabularFunction extends DefinedFunction
{
  //-------------------------------------------------------------------
  //  The table
  //-------------------------------------------------------------------
  private double[] xValues;
  private double[] yValues;

  //-------------------------------------------------------------------
  //  Dimension of the result
  //-------------------------------------------------------------------
  private String tableunit;


  //=====================================================================
  //  Return signum of double float number 'n'.
  //=====================================================================
  private static int signum(double n)
    { return n==0? 0 : (n>0? 1 : -1); }


  //=====================================================================
  //  Construct object for function 'nam' defined at 'loc'.
  //  Dimension of the result is defined by unit expression 'u',
  //  and the table is given by the Vectors 'x' and 'y'.
  //=====================================================================
  TabularFunction
    ( String nam, Location loc,
      String u, Vector<Double> x, Vector<Double> y)
    {
      super(nam,loc);
      tableunit = u;
      int n = x.size();
      xValues = new double[n];
      yValues = new double[n];
      for (int i=0;i<n;i++)
      {
        xValues[i] = x.elementAt(i);
        yValues[i] = y.elementAt(i);
      }
    }


  //=====================================================================
  //  Given is a line from units.dat file, parsed into
  //  name 'nam' and definition 'df'. If this line defines a tabular
  //  function, construct a TabularFunction object defined by it,
  //  it into user function table, and return true. Otherwise return false.
  //=====================================================================
  public static boolean accept
    ( final String nam, final String df, Location loc)
    {
      //  If unit name contains '[', we have a table definition.

      int leftParen = nam.indexOf('[');
      int rightParen = nam.indexOf(']',leftParen+1);

      if (leftParen<0) return false;

      // Get function name and unit

      if (rightParen!=nam.length()-1
           || rightParen==leftParen+1)
      {
        Env.err.println
          ("Bad function definition of '" + nam + "' on line "
            + loc.lineNum + " ignored.");
        return true;
      }

      String funcname = nam.substring(0,leftParen);
      String tabunit = nam.substring(leftParen+1,rightParen);

      // Is it redefinition?

      if (table.containsKey(funcname))
      {
        Env.err.println
          ("Redefinition of function '" + funcname
            + "' on line " + loc.lineNum + " is ignored.");
        return true;
      }

      Vector<Double> x = new Vector<Double>();
      Vector<Double> y = new Vector<Double>();

      int p = 0;
      while (p<df.length())
      {
        int q = Util.strtod(df,p);
        if (p==q) break;

        x.addElement(new Double(df.substring(p,q).trim()));
        p = q;

        q = Util.strtod(df,p);
        if (p==q)
        {
          Env.err.println
            ("Missing last value after "
              + x.lastElement().doubleValue()
              + ".\n"
              + "Definition of function '" + nam
              + "' on line " + loc.lineNum + " is ignored.");
          return true;
        }

        y.addElement(new Double(df.substring(p,q).trim()));
        p = q;

        if (p>=df.length()) break;
        if (df.charAt(p)==',') p++;
      }

      // Install function in table.

      table.put(funcname,
              new TabularFunction(funcname,loc,tabunit,x,y));
      return true;
    }


  //=====================================================================
  //  Apply the function to Value 'v' (with result in 'v').
  //=====================================================================
  void applyTo(Value v)
    {
      Value dim = null;
      try
      { dim = Value.parse(tableunit); }

      catch (EvalError e)
      {
        throw new EvalError("Invalid dimension, " + dim +
                         ", of function " + name + ". " + e.getMessage());
      }

      if (!v.isNumber())
        throw new EvalError("Argument " + v.asString() + " of " +
                         name + " is not a number.");

      double result = interpolate(v,v.factor,xValues,yValues,"");

      dim.factor *= result;
      v.copyFrom(dim);
    }


  //=====================================================================
  //  Apply inverse of the function to Value 'v' (with result in 'v').
  //=====================================================================
  public void applyInverseTo(Value v)
    {
      Value dim = null;
      try
      { dim = Value.parse(tableunit); }

      catch (EvalError e)
      {
        throw new EvalError("Invalid dimension, " + dim +
                         ", of function ~" + name + ". " + e.getMessage());
      }

      Value n = new Value(v);
      n.div(dim);

      if (!n.isNumber())
        throw new EvalError("Argument " + v.asString() +
                      " of function ~" + name + " is not conformable to " +
                        dim.asString() + ".");

      double result = interpolate(v,n.factor,yValues,xValues,"~");

      v.copyFrom(new Value());
      v.factor = result;
    }

  //=====================================================================
  //  Return definition of the function.
  //  (Originally 'showfuncdef'.)
  //=====================================================================
  String showdef()
    {
      String pref = ("0123456789.".indexOf(tableunit.charAt(0))>=0)? " * " : " ";
      boolean nc = Env.verbose>0; // not compact?

      StringBuffer sb = new StringBuffer();
      if (Env.verbose>0) sb.append("\tDefinition: interpolated table with points");
      else sb.append("Interpolated table with points:");

      for(int i=0;i<xValues.length;i++)
        sb.append((Env.verbose>0? "\n\t\t    " : "\n ") + name
                     + "(" + xValues[i]+ ") = " + yValues[i] + pref + tableunit);
      return sb.toString();
    }


  //=====================================================================
  //  Check the definition. Used in 'checkunits'.
  //=====================================================================
  void check()
    {
      if (Env.verbose==2)
        Env.out.println("doing function " + name);

      // Check for monotonicity which is needed for unique inverses
      if (xValues.length<=1)
      {
        Env.out.println
          ("Table '" + name + "' has only one data point");
        return;
      }
      int direction = signum(yValues[1]-yValues[0]);
      for(int i=2;i<xValues.length;i++)
        if (direction==0 || signum(yValues[i]-yValues[i-1]) != direction)
        {
          Env.out.println
            ("Table '" + name + "' lacks unique inverse around entry "
              + Util.shownumber(xValues[i-1]));
          return;
        }
      return;
    }

    public Value getConformability(){
        return tableunit != null ? Value.fromString(tableunit) : null;
    }
  //=====================================================================
  //  Return true if this function is compatible with Value 'v',
  //=====================================================================
  boolean isCompatibleWith(final Value v)
    {
      Value thisvalue = Value.fromString(tableunit);
      if (thisvalue==null) return false;
      return thisvalue.isCompatibleWith(v,Factor.Ignore.DIMLESS);
    }


  //=====================================================================
  //  Return short description of this object to be shown by 'tryallunits'.
  //=====================================================================
  String desc()
    { return "<piecewise linear unit>"; }


  //=====================================================================
  //  The arrays 'x' and 'y' contain x-values and corresponding y-values.
  //  Find, by linear interpolation, the y-value corresponding to the
  //  x-value given by the factor of Value object 'v'.
  //  The argument 'inv' is either empty string or '~', and is used
  //  in the error message to show if we are doing inverse or not.
  //=====================================================================
  private double interpolate(Value v, double xval, double[] x, double[] y, String inv)
    {
      for(int i=0;i<x.length-1;i++)
        if ((x[i]<=xval && xval<=x[i+1]) || (x[i]>=xval && xval>=x[i+1]))
          return y[i] + (xval-x[i])*(y[i+1]-y[i])/(x[i+1]-x[i]);

      throw new EvalError("Argument " + v.asString() +
                     " is outside the domain of " + inv + name + ".");
    }
}
