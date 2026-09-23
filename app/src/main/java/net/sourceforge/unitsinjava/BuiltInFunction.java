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
//    050203 Version 1.84.J05. Do not initialize table.
//    050315 Version 1.84.J07. Changed package name to "units".
//    091024 Version 1.87.J01
//           Used generics for 'table'.
//    091025 Replaced 'Parser.Exception' by 'EvalError'.
//    091027 Added 'sqrt' and 'cuberoot'.
//           Used Enum.
//           Added check for undefined result of math functions.
//    091031 Replaced 'addtolist' by 'isCompatibleWith'.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Hashtable;
import java.util.Vector;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class BuiltInFunction
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A built-in function.
 */

 public class BuiltInFunction extends Function
{
  //-------------------------------------------------------------------
  /**  Table of built-in functions. */
  //-------------------------------------------------------------------
  public static Hashtable<String,BuiltInFunction> table = null;

  //-------------------------------------------------------------------
  //  Argument and result types
  //-------------------------------------------------------------------
  private type funcType;

  private enum type
    {
      DIMLESS,  // Argument and result are numbers
      ANGLEIN,  // Argument is a number or an angle, result a number.
      ANGLEOUT, // Argument is a number, result an angle.
      NOCHECK   // The invoked procedure checks types.
    } ;

  //-------------------------------------------------------------------
  //  Because Java does not have pointers, the procedure invoked
  //  to evaluate the function is identified by enumeration.
  //-------------------------------------------------------------------
  private proc procID;

  private enum proc {SIN,COS,TAN,LN,LOG,LOG2,EXP,ASIN,ACOS,ATAN,SQRT,CBRT};

  //-------------------------------------------------------------------
  //  Radian (an angle is a unit reducible to radians)
  //-------------------------------------------------------------------

  private static Unit radian;


  //=====================================================================
  //  Construct object for built-in function 'name'
  //  of type 'funcType' with procedure ID 'procID'.
  //=====================================================================
  BuiltInFunction(String name, type funcType, proc procID)
    {
      super(name,new Location());
      this.funcType = funcType;
      this.procID   = procID;
    }

  //=====================================================================
  //  Insert into the table a built-in function 'name'
  //  of type 'funcType' with procedure ID 'procID'.
  //=====================================================================
  private static void insert(String name, type funcType, proc procID)
    { table.put(name, new BuiltInFunction(name,funcType,procID)); }


  //=====================================================================
  //  Fill table of built-in functions.
  //=====================================================================
  public static void makeTable()
    {
      insert("sin",     type.ANGLEIN, proc.SIN);
      insert("cos",     type.ANGLEIN, proc.COS);
      insert("tan",     type.ANGLEIN, proc.TAN);
      insert("ln",      type.DIMLESS, proc.LN);
      insert("log",     type.DIMLESS, proc.LOG);
      insert("log2",    type.DIMLESS, proc.LOG2);
      insert("exp",     type.DIMLESS, proc.EXP);
      insert("acos",    type.ANGLEOUT,proc.ACOS);
      insert("atan",    type.ANGLEOUT,proc.ATAN);
      insert("asin",    type.ANGLEOUT,proc.ASIN);
      insert("sqrt",    type.NOCHECK, proc.SQRT);
      insert("cuberoot",type.NOCHECK, proc.CBRT);

      //---------------------------------------------------------------
      //  Make sure radian is defined and save its object.
      //---------------------------------------------------------------
      Unit r = Unit.table.get("radian");
      if (r!=null) radian = r;
      else r = new Unit("radian",new Location(),"!");
    }


  //=====================================================================
  //  Apply the function to Value 'v' (with result in 'v').
  //=====================================================================
  void applyTo(Value v)
    {
      //---------------------------------------------------------------
      //  Check argument type
      //---------------------------------------------------------------
      switch (funcType)
      {
        case ANGLEIN:   //-------- Must be a number or an angle
          if (!v.isNumber())
          {
            String s = v.asString();
            v.denominator.add(radian);
            if (!v.isNumber())
              throw new EvalError("Argument " + s + " of " +
                            name + " is not a number or angle.");
          }
          break;

        case ANGLEOUT:  //-------- Must be a number
        case DIMLESS:
          if (!v.isNumber())
            throw new EvalError("Argument " + v.asString() + " of " +
                            name + " is not a number.");
          break;

        case NOCHECK:   //-------- No checking
          break;

        default:        //-------- No other type exists
          throw new Error("Program Error; funcType=" + funcType);
      }

      //---------------------------------------------------------------
      //  Apply the function
      //---------------------------------------------------------------
      double arg = v.factor;
      switch (procID)
      {
        case SIN:  v.factor = Math.sin(arg); break;
        case COS:  v.factor = Math.cos(arg); break;
        case TAN:  v.factor = Math.tan(arg); break;
        case LN:   v.factor = Math.log(arg); break;
        case LOG:  v.factor = Math.log(arg)/Math.log(10); break;
        case LOG2: v.factor = Math.log(arg)/Math.log(2); break;
        case EXP:  v.factor = Math.exp(arg); break;
        case ASIN: v.factor = Math.asin(arg); break;
        case ACOS: v.factor = Math.acos(arg); break;
        case ATAN: v.factor = Math.atan(arg); break;
        case SQRT: v.root(2); break;
        case CBRT: v.root(3); break;
        default: throw new Error("Program Error; procID=" + procID);
      }

      //---------------------------------------------------------------
      //  Check result
      //---------------------------------------------------------------
      double result = v.factor;
      boolean NaN = Double.isNaN(result) | Double.isInfinite(result);

      switch (funcType)
      {
        case ANGLEIN:   //-------- Must be a number
        case DIMLESS:
          if (NaN)
            throw new EvalError("The result of " +
                            name + " is undefined.");
          break;

        case ANGLEOUT:  //-------- Must be an angle
          if (NaN)
            throw new EvalError("The result of " +
                            name + " is undefined.");
            v.numerator.add(radian);
          break;

        case NOCHECK:   //-------- No checking
          break;

        default:        //-------- No other type exists
          throw new Error("Program Error; funcType=" + funcType);
      }
    }


  //=====================================================================
  //  These methods, defined in Entity and Function classes,
  //  are never invoked for a BuiltInFunction.
  //=====================================================================
  public void applyInverseTo(Value v)
    { throw new Error("Program Error"); }

  String showdef()
    { throw new Error("Program Error"); }

  void check()
    { throw new Error("Program Error"); }

  boolean isCompatibleWith(final Value v)
    { throw new Error("Program Error"); }

  String desc()
    { throw new Error("Program Error"); }
}
