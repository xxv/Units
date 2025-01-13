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
//  Version 1.84.J05.
//    050203 Do not initialize table.
//
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.87.J01.
//    091024 Used generics for 'table'.
//
//  Version 1.89.J01.
//    120311 Added static method 'showdef'.
//    120312 Moved here from Value the method 'convert'.
//    120318 Added method 'checkHiding'.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Hashtable;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class DefinedFunction
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A defined function.
 *  Base class for ComputedFunction and TabularFunction.
 */

public abstract class DefinedFunction extends Function
{
  //-------------------------------------------------------------------
  /**  Table of defined functions. */
  //-------------------------------------------------------------------
  public static Hashtable<String,DefinedFunction> table = null;

  public abstract Value getConformability();

  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs DefinedFunction object defined in a units file.
   *
   *  @param name function name.
   *  @param loc  location in the file.
   */
  DefinedFunction(String name, Location loc)
    { super(name,loc); }


  //=====================================================================
  //  checkHiding
  //=====================================================================
  /**
   *  Prints a message if name of this function is identical
   *  with the name of a unit or prefix.
   *  Used by method 'check' in the subclasses.
   */
  void checkHiding()
    {
      Unit unit = Unit.table.get(name);
      if (unit!=null)
        Env.out.println
          (location.where() + ". Function '" + name +
           "' hides the unit defined in " +
           unit.location.where() + ".");

      Prefix pref = Prefix.table.get(name);
      if (pref!=null)
        Env.out.println
          (location.where() + ". Function '" + name +
           "' hides the prefix defined in " +
           pref.location.where() + ".");
    }


  //=====================================================================
  //  showdef
  //=====================================================================
  /**
   *  If the argument is the name of a defined function
   *  returns its definition followed by equal sign.
   *  Otherwise returns null.
   *
   *  @param  name the name to be checked.
   *  @return the definition or null.
   */
  public static String showdef(final String name)
    {
      Function func = DefinedFunction.table.get(name);
      if (func!=null) return func.showdef();
      else return null;
    }


  //=====================================================================
  //  convert
  //=====================================================================
  /**
   *  Converts a unit expression to this function.
   *  It shows the argument of the function that
   *  produces the value specified by the expression.
   *  The result is written to Env.out.
   *  Writes error message to Env.out if conversion fails.
   *
   *  @param  fromExpr the expression.
   *  @param  fromValue the expression evaluated to completely reduced Value.
   *  @return true if conversion was successful, false otherwise.
   */
  boolean convert(String fromExpr, Value fromValue)
    {
      //---------------------------------------------------------------
      //  The result is obtained by applying inverse
      //  of this function to the given value.
      //---------------------------------------------------------------
      try
      {
        applyInverseTo(fromValue);
        fromValue.completereduce();
      }
      catch(EvalError e)
      {
        Env.out.println(e.getMessage());
        return false;
      }

      //---------------------------------------------------------------
      //  Write out the result formatted as required.
      //---------------------------------------------------------------
      if (Env.verbose==2)
        Env.out.print("\t" + fromExpr + " = " + name + "(");
      else
        if (Env.verbose==1) Env.out.print("\t");
      Env.out.print(fromValue.asString());
      if (Env.verbose==2)
        Env.out.print(")");
      Env.out.print("\n");
      return true;
    }


}