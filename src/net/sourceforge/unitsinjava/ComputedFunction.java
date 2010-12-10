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
//    070102 Suppress printing "\tDefinition :" for compact output.
//    091024 Version 1.87.J01. Used generics for 'list' in 'addtolist'.
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
//  class ComputedFunction
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A function defined by an expression (a 'nonlinear unit'.)
 */

 public class ComputedFunction extends DefinedFunction
{
  private FuncDef forward; // Forward definition
  private FuncDef inverse; // Inverse definition


  //=====================================================================
  //  Construct object for function 'nam' defined at 'loc'.
  //  Forward function is defined by string 'fdf', and has parameter 'fpar'
  //  of dimension 'fdim'.
  //  Inverse function is defined by string 'idf', and has parameter 'ipar'
  //  of dimension 'idim'.
  //=====================================================================
  ComputedFunction
    ( String nam, Location loc,
      String fpar, String fdf, String fdim,
      String ipar, String idf, String idim)
    {
      super(nam,loc);
      forward = new FuncDef(fpar,fdf,fdim);
      inverse = new FuncDef(ipar,idf,idim);
    }


  //=====================================================================
  //  Given is a line from units.dat file, parsed into
  //  name 'nam' and definition 'df'. If this line defines a computed
  //  function, construct a ComputedFunction object defined by it, enter
  //  it into user function table, and return true. Otherwise return false.
  //=====================================================================
  public static boolean accept
    ( final String nam, final String df, Location loc)
    {
      //  If unitname contains '(', we have a function definition.
      //  Syntax is: funcname(param) [fwddim;invdim] fwddef ; invdef
      //  [fwddim;invdim] may be omitted.
      //  ; invdef may be omitted.


      int leftParen = nam.indexOf('(');
      int rightParen = nam.indexOf(')',leftParen+1);

      if (leftParen<0) return false;

      // Get function name and parameter

      if (rightParen!=nam.length()-1
          || rightParen==leftParen+1)
      {
        Env.err.println
          ("Bad function definition of '" + nam + "' on line "
            + loc.lineNum + " ignored.");
        return true;
      }

      String funcname = nam.substring(0,leftParen);
      String param = nam.substring(leftParen+1,rightParen);

      // Get dimensions

      String unitdef = df;
      String fwddim = null;
      String invdim = null;

      if (df.charAt(0)=='[')
      {
        int semicol = df.indexOf(';',1);
        int rightBracket = df.indexOf(']',semicol+1);
        if (semicol<0 || rightBracket<0)
        {
          Env.err.println
            ("Bad dimension of function '" + nam + "' on line " +
             loc.lineNum + ".Function ignored.");
          return true;
        }
        if (semicol>1)
          fwddim = df.substring(1,semicol);
        if (rightBracket-semicol>1)
          invdim = df.substring(semicol+1,rightBracket);

        unitdef = df.substring(rightBracket+1,df.length()).trim();
      }

      // Get definitions

      String fwddef = null;
      String invdef = null;

      int semicol = unitdef.indexOf(';');

      if (semicol<0)
        fwddef = unitdef;

      else
      {
        fwddef = unitdef.substring(0,semicol).trim();
        invdef = unitdef.substring(semicol+1,unitdef.length()).trim();
      }

      // Is it redefinition?

      if (table.containsKey(funcname))
      {
        Env.err.println
          ("Redefinition of function '" + funcname
            +"' on line " + loc.lineNum + " is ignored.");
        return true;
      }

      // Install function in table.

      table.put(funcname,
              new ComputedFunction(funcname,loc,
                                   param,fwddef,fwddim,
                                   funcname,invdef,invdim));
      return true;
    }


  //=====================================================================
  //  Apply the function to Value 'v' (with result in 'v').
  //=====================================================================
  void applyTo(Value v)
    { forward.applyTo(v,""); }


  //=====================================================================
  //  Apply inverse of the function to Value 'v' (with result in 'v').
  //=====================================================================
  public void applyInverseTo(Value v)
    { inverse.applyTo(v,"~"); }


  //=====================================================================
  //  Return definition of the function.
  //  (Originally 'showfuncdef'.)
  //=====================================================================
  String showdef()
    {
      return (Env.verbose>0? "\tDefinition: " : "") + name
                    + "(" + forward.param + ") = " + forward.def;
    }


  //=====================================================================
  //  Check the function. Used in 'checkunits'.
  //=====================================================================
  void check()
    {
      if (Env.verbose==2)
        Env.out.println("doing function " + name);


      Value v;

      if (forward.dimen!=null)
      {
        try
        {
          v = Value.parse(forward.dimen);
          v.completereduce();
        }
        catch (EvalError e)
        {
          Env.out.println
            ("Function '" + name + "' has invalid type '"
              + forward.dimen + "'");
          return;
        }
      }

      else
        v = new Value();

      v.factor *= 7; // Arbitrary choice where we evaluate inverse

      Value saved = new Value(v);

      try
      { applyTo(v); }
      catch (EvalError e)
      {
        Env.out.println
          ("Error in definition " + name + "("
            + forward.param + ") as " + forward.def);
        return;
      }

      if (inverse.def==null)
      {
        Env.out.println
          ("Warning: no inverse for function '" + name + "'");
        return;
      }

      try
      {
        applyInverseTo(v);
        v.div(saved);
        v.completereduce();
        double delta = v.factor-1;
        if (!v.isNumber() || delta<-1e-12 || delta>1e-12)
          Env.out.println
            ("Inverse is not the inverse for function '" + name + "'");
      }
      catch (EvalError e)
      {
        Env.out.println
          ("Error in inverse ~" + name + "("
            + inverse.param + ") as " + inverse.def);
      }

    }

    public Value getConformability(){
        return inverse.dimen != null ? Value.fromString(inverse.dimen) : null;
    }
  //=====================================================================
  //  Return true if this function is compatible with Value 'v',
  //=====================================================================
  boolean isCompatibleWith(final Value v)
    {
      if (inverse.dimen==null) return false;
      Value thisvalue = Value.fromString(inverse.dimen);
      if (thisvalue==null) return false;
      return thisvalue.isCompatibleWith(v,Factor.Ignore.DIMLESS);
    }


  //=====================================================================
  //  Return short description of this object to be shown by 'tryallunits'.
  //=====================================================================
  String desc()
    { return "<nonlinear unit>"; }


  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Inner class FuncDef
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  /**
   *  Holds details of a forward or inverse definition of the function.
   */
  private class FuncDef
  {
    String param;
    String def;
    String dimen;

    //-----------------------------------------------------------------
    //  Construct FuncDef object for definition srting 'df'
    //  with parameter 'par' of dimension 'dim'.
    //-----------------------------------------------------------------
    FuncDef(String par, String df, String dim)
      {
        param = par;
        def = df;
        dimen = dim;
      }

    //-----------------------------------------------------------------
    //  Apply the function (or inverse) defined by the object
    //  to Value 'v'.
    //-----------------------------------------------------------------
    void applyTo(Value v, String inv)
      {
        v.completereduce();
        if (dimen!=null)
        {
          Value dim;
          try
          { dim = Value.parse(dimen); }
          catch (EvalError e)
          {
            throw new EvalError("Invalid dimension, " + dimen +
                            ", of function " + inv + name + ". " + e.getMessage());
          }
          dim.completereduce();
          if (!dim.isCompatibleWith(v,Factor.Ignore.NONE))
          throw new EvalError("Argument " + v.asString() +
                            " of function " + inv + name + " is not conformable to " +
                            dim.asString() + ".");
        }

        Value result;
        try
        { result = Value.parse(def,param,v); }
        catch (EvalError e)
        {
          throw new EvalError("Invalid definition of function '" +
                           inv + name + "'. " + e.getMessage());
        }

        v.copyFrom(result);
      } // end applyTo

  } // end FuncDef
}
