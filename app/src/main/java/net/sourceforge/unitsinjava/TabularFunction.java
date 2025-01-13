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
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.86.J01.
//    061229 Corrected test for 'verbose'.
//    070102 Suppress printing "\tDefinition :" and tabs for compact output.
//
//  Version 1.87.J01.
//    091024 Used modified 'insertAlph'.
//           Used generics for 'x'.'y'.
//    091025 Replaced 'Parser.Exception' by 'EvalError'.
//    091031 Moved definition of Ignore to Factor.
//           Replaced 'addtolist' by 'isCompatibleWith'.
//
//  Version 1.88.J02
//    110403 In 'showdef': removed optional "\tDefinition :";
//           replaced StringBuffer by StringBuilder;
//           removed unused variable 'nc'.
//
//  Version 1.89.J01
//    120201 Adapted to use with File Parser:
//           removed method 'accept' and added 'define'.
//    120208 Renamed one-argument method 'isCompatibleWith'
//           to 'conformsTo' to avoid confusion with two-argument one
//           defined in Product and Value.
//    120209 Definition of Ignore moved to separate file:
//           replaced 'Factor.Ignore' by 'Ignore'.
//    120311 Suppress error messages from 'conformsTo'.
//    120313 Added 'location.where' to messages from 'check'.
//    120317 Changed 'define' to replace an earlier definition
//           instead of ignoring re-definition.
//    120318 In 'check': check name conflict using 'checkHiding'.
//    120404 In 'check': added checking of result unit.
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
  private final double[] x; // Argument values
  private final double[] y; // Corresponding values in units 'resUnit'.

  //-------------------------------------------------------------------
  //  Result unit
  //-------------------------------------------------------------------
  private final String resUnit;


  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs TabularFunction object.
   *
   *  @param name    function name.
   *  @param loc     location where defined.
   *  @param resUnit unit for y-values.
   *  @param x       argument values.
   *  @param y       corresponding values in units 'resUnit'.
   */
  TabularFunction
    ( final String name, final Location loc,
      final String resUnit, final double[] x, final double[] y)
    {
      super(name,loc);
      this.resUnit = resUnit;
      this.x = x;
      this.y = y;
    }


  //=====================================================================
  //  define
  //=====================================================================
  /**
   *  Builds FunctionTable entry from a parsed definition.
   *  The definition is parsed as follows:
   *  <pre>
   *    name[resUnit]  x1 y1, x2 y2, ... , xn yn
   *  </pre>
   *
   *  @param name  function name.
   *  @param resUnit unit for y-values.
   *  @param x     argument values.
   *  @param y     corresponding values in units 'resUnit'.
   *  @param loc   location where defined.
   */
  public static void define
    ( final String name, final String resUnit,
      final double[] x, final double[] y, final Location loc)
    {
      //---------------------------------------------------------------
      // Function with incorrect name can never be accessed.
      //---------------------------------------------------------------
      String diag = Entity.checkName(name);

      if (diag!=null)
      {
         Env.out.println
           (loc.where() + ". Function '" + name
            + "' is ignored. Its name " + diag + ".");
         return;
      }

      //---------------------------------------------------------------
      //  Install the function in table.
      //---------------------------------------------------------------
      Function old = table.put(name, new TabularFunction(name,loc,resUnit,x,y));

      //---------------------------------------------------------------
      //  Write a message if an earlier definition replaced.
      //---------------------------------------------------------------
      if (old!=null)
      {
        Env.out.println
          ("Function '" + name + "' defined in " + old.location.where() +
           ", is redefined in " + loc.where() + ".");
      }
    }


  //=====================================================================
  //  applyTo
  //=====================================================================
  /**
   *  Applies this function to a given Value,
   *  and changes the Value to the result.
   *
   *  @param v the argument and result.
   */
   void applyTo(Value v)
    {
      //---------------------------------------------------------------
      //  Parse resUnit to obtain a Value, 'dim'.
      //---------------------------------------------------------------
      Value dim = null;
      try
      { dim = Value.parse(resUnit); }

      catch (EvalError e)
      {
        throw new EvalError("Invalid result unit, " + resUnit +
                         ", of function " + name + ". " + e.getMessage());
      }

      //---------------------------------------------------------------
      //  The argument must be a number.
      //---------------------------------------------------------------
      if (!v.isNumber())
        throw new EvalError("Argument " + v.asString() + " of '" +
                         name + "' is not a number.");

      //---------------------------------------------------------------
      //  Find y-value corresponding to argument 'x'.
      //---------------------------------------------------------------
       double result = interpolate(v.factor,x,y,v,"");

      //---------------------------------------------------------------
      //  Return result in result units.
      //---------------------------------------------------------------
      dim.factor *= result;
      v.copyFrom(dim);
    }


  //=====================================================================
  //  applyInverseTo
  //=====================================================================
  /**
   *  Applies the inverse of this function to a given Value,
   *  and changes the Value to the result.
   *
   *  @param v the argument and result.
   */
  public void applyInverseTo(Value v)
    {
      //---------------------------------------------------------------
      //  Parse resUnit to obtain a Value, 'dim'.
      //---------------------------------------------------------------
      Value dim = null;
      try
      { dim = Value.parse(resUnit); }

      catch (EvalError e)
      {
        throw new EvalError("Invalid result unit, '" + resUnit +
                         "', of '" + name + "'. " + e.getMessage());
      }

      //---------------------------------------------------------------
      //  Express argument as n*resUnit.
      //---------------------------------------------------------------
      Value n = new Value(v);
      n.div(dim);

      //---------------------------------------------------------------
      //  'n' must be a number.
      //---------------------------------------------------------------
      if (!n.isNumber())
        throw new EvalError("Argument " + v.asString() +
                      " of '~" + name + "' is not conformable to '" +
                        resUnit + "'.");

      //---------------------------------------------------------------
      //  Find x-value corresponding to y-value 'n'.
      //---------------------------------------------------------------
      double result = interpolate(n.factor,y,x,v,"~");

      //---------------------------------------------------------------
      //  Return result as Value.
      //---------------------------------------------------------------
      v.copyFrom(new Value());
      v.factor = result;
    }

  //=====================================================================
  //  showdef
  //=====================================================================
  /**
   *  Returns definition of this function.
   *  (Originally 'showfuncdef'.)
   *
   *  @return formatted definition of this function.
   */
  String showdef()
    {
      String pref = ("0123456789.".indexOf(resUnit.charAt(0))>=0)? " * " : " ";

      StringBuilder sb = new StringBuilder("Interpolated table with points:");

      for(int i=0;i<x.length;i++)
        sb.append((Env.verbose>0? "\n\t\t    " : "\n ") + name
                     + "(" + x[i]+ ") = " + y[i] + pref + resUnit);
      return sb.toString();
    }


  //=====================================================================
  //  check
  //=====================================================================
  /**
   *  Checks definition of this function for correctness.
   *  Writes diagnostics to 'Env.out'.
   *  Used by 'check' in 'Tables'.
   */
  void check()
    {
      if (Env.verbose==2)
        Env.out.println(location.where() + ". Doing function " + name);

      //---------------------------------------------------------------
      //  Function name must be different from that of alias, unit,
      //  and prefix. Conflict with alias is checked by Alias.
      //  We check here for conflict with unit and prefix.
      //---------------------------------------------------------------
      checkHiding();

      //---------------------------------------------------------------
      //  Check result unit
      //---------------------------------------------------------------
      try
      { Value.parse(resUnit); }

      catch (EvalError e)
      {
        Env.out.println
          (location.where() + ". Invalid result unit, '" + resUnit +
           "', of '" + name + "'. " + e.getMessage());
      }

      //---------------------------------------------------------------
      // Check that at least two points are defined.
      //---------------------------------------------------------------
      if (x.length<=1)
      {
        Env.out.println
          (location.where() + ". Table '" + name +
           "' has only one data point.");
        return;
      }

      //---------------------------------------------------------------
      // Check for monotonicity which is needed for unique inverses
      //---------------------------------------------------------------
      int direction = signum(y[1]-y[0]);
      for(int i=2;i<x.length;i++)
        if (direction==0 || signum(y[i]-y[i-1]) != direction)
        {
          Env.out.println
            (location.where() + ". Table '" + name +
             "' lacks unique inverse around entry " +
             Util.shownumber(x[i-1]) + ".");
          return;
        }
    }

    public Value getConformability() {
        return resUnit != null ? Value.fromString(resUnit) : null;
    }


  //=====================================================================
  //  conformsTo
  //=====================================================================
  /**
   *  Checks if result of this function conforms to Value 'v'.
   *  Used by 'showConformable' in 'Tables'.
   *
   *  @param  v the Value to be checked against.
   *  @return true if this function conforms to v, false otherwise.
   */
  boolean conformsTo(final Value v)
    {
      try
      {
        Value thisvalue = Value.parse(resUnit);
        thisvalue.completereduce();
        return thisvalue.isCompatibleWith(v,Ignore.DIMLESS);
      }
      catch(EvalError e)
      { return false; }
    }


  //=====================================================================
  //  desc
  //=====================================================================
  /**
   *  Returns short description of this function
   *  to be shown by 'showConformable' and 'showMatching' in 'Tables'.
   *
   *  @return description.
   */
  String desc()
    { return "<function>"; }


  //=====================================================================
  //  interpolate
  //=====================================================================
  /**
   *  Finds by interpolation in tables an output value corresponding
   *  to a given input value. Thhrow EvalError exception if input value
   *  is outside the inputs table.
   *
   *  @param inval numeric input value.
   *  @param in    table of input values.
   *  @param out   table of output values.
   *  @param v     input value with units, for diagnostics.
   *  @param inv   empty string or '~', for disagnostics.
   */
  private double interpolate(double inval, double[] in, double[] out, Value v, String inv)
    {
      for(int i=0;i<in.length-1;i++)
        if ((in[i]<=inval && inval<=in[i+1]) || (in[i]>=inval && inval>=in[i+1]))
          return out[i] + (inval-in[i])*(out[i+1]-out[i])/(in[i+1]-in[i]);

      throw new EvalError("Argument " + v.asString() +
                     " is outside the domain of '" + inv + name + "'.");
    }


  //=====================================================================
  //  signum
  //=====================================================================
  /**
   *  Finds signum of a given number.
   *
   *  @param  d a number.
   *  @return -1, 0, or +1 if, respectively, d<0, d==0, or d>0.
   */
  private static int signum(double d)
    { return d==0? 0 : (d>0? 1 : -1); }
}
