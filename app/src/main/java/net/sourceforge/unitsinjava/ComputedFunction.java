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
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.86.J01.
//    061229 Corrected test for 'verbose'.
//    070102 Suppress printing "\tDefinition :" for compact output.
//
//  Version 1.87.J01.
//    091024 Used generics for 'list' in 'addtolist'.
//    091025 Replaced 'Parser.Exception' by 'EvalError'.
//    091031 Moved definition of Ignore to Factor.
//           Replaced 'addtolist' by 'isCompatibleWith'.
//
//  Version 1.88.J02
//    110403 Removed optional "\tDefinition :" from 'showdef'.
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
//    120316 Corrected message about abandoned inverse check.
//    120317 Changed 'define' to replace an earlier definition
//           instead of ignoring re-definition.
//    120318 In 'check': check name conflict using 'checkHiding'.
//    120402 In 'check': added checking of inverse parameter.
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
  //  Constructor
  //=====================================================================
  /**
   *  Constructs ComputedFunction object.
   *
   *  @param nam  function name.
   *  @param loc  location where defined.
   *  @param fdf  forward function definition.
   *  @param fpar forward function parameter.
   *  @param fdim dimension of forward function parameter.
   *  @param idf  inverse function definition.
   *  @param ipar inverse function parameter.
   *  @param idim dimension of inverse function parameter.
   */
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
  //  define
  //=====================================================================
  /**
   *  Builds FunctionTable entry from a parsed definition.
   *  The definition is parsed as follows:
   *  <pre>
   *    name(param)  [fwddim;invdim] fwddef ; invdef
   *  </pre>
   *
   *  @param name   function name.
   *  @param param  parameter.
   *  @param fwddim dimension of forward function parameter.
   *  @param invdim dimension of inverse function parameter.
   *  @param fwddef forward function definition.
   *  @param invdef inverse function definition.
   *  @param loc    location where defined.
   */
  public static void define
    ( final String name, final String param,
      final String fwddim, final String invdim,
      final String fwddef, final String invdef,
      final Location loc)
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
      Function old = table.put(name,new ComputedFunction
                                          (name,loc,
                                           param,fwddef,fwddim,
                                           name,invdef,invdim));

      //---------------------------------------------------------------
      //  Write a message if an earlier definition is replaced.
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
    { forward.applyTo(v,""); }


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
    { inverse.applyTo(v,"~"); }

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
    { return name + "(" + forward.param + ") = " + forward.def; }


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
      //  Parse dimension of inverse argument to check it.
      //  Notice that this is dimension of function's result.
      //  Null dimension means no check.
      //---------------------------------------------------------------
      if (inverse.dimen!=null)
      {
        try
        { Value.parse(inverse.dimen); }
        catch (EvalError e)
        {
          Env.out.println
            (location.where() + ". Dimension '" + inverse.dimen +
             "' specified for argument of '~" + name +
             "' is invalid. " + e.getMessage() + ".");
        }
      }

      //---------------------------------------------------------------
      //  Parse dimension of forward argument to check it,
      //  and save its value for checking of inverse.
      //  Null dimension means no check.
      //---------------------------------------------------------------
      Value v = new Value();  // Set 'v' to number 1

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
            (location.where() + ". Dimension '" + forward.dimen +
             "' specified for argument of '" + name +
             "' is invalid. " + e.getMessage() + ".");
          return;
        }
      }

      //---------------------------------------------------------------
      //  If inverse not defined: issue warning and return.
      //---------------------------------------------------------------
      if (inverse.def==null)
      {
        Env.out.println
          (location.where() + ". Warning: no inverse for function '" +
           name + "'.");
        return;
      }

      //---------------------------------------------------------------
      //  Forward argument is correct and inverse is defined.
      //  We proceed to check the inverse.
      //  The Value 'v' is now 1 with the dimension of forward argument.
      //  (Number 1 was assumed for null dimension.)
      //  Set 'v' to an arbitrarily chosen value
      //  where we are going to check the inverse.
      //---------------------------------------------------------------
      v.factor *= 7;

      //---------------------------------------------------------------
      //  Save 'v' for future comparison and apply the function to it.
      //  If the function cannot be evaluated for 'v', issue a warning
      //  and return. (That function cannot be evaluated does not mean
      //  it is faulty. The argument 7 may be out of accepted range.)
      //---------------------------------------------------------------
      Value saved = new Value(v);

      try
      { applyTo(v); }
      catch (EvalError e)
      {
        Env.out.println
          (location.where() +
           ". Warning: inverse was not checked for '" +
           name + "'.");
        return;
      }

      //---------------------------------------------------------------
      //  'v' contains now the evaluated function. Apply the function's
      //  inverse to it and issue a message if the result differs from
      //  the saved value by more than a factor of 10^-12.
      //  Issue an error message if the inverse cannot be evaluated.
      //---------------------------------------------------------------
      try
      {
        applyInverseTo(v);
        v.div(saved);
        v.completereduce();
        double delta = v.factor-1;
        if (!v.isNumber() || delta<-1e-12 || delta>1e-12)
          Env.out.println
            (location.where() +
             ". Inverse is not the inverse for function '" +
             name + "'.");
      }
      catch (EvalError e)
      {
        Env.out.println
          (location.where() + ". Error in '~" + name + "(" +
           inverse.param + ")' defined as '" + inverse.def + "'.");
      }
    }

    public Value getConformability() {
      return inverse.dimen != null ? Value.fromString(inverse.dimen) : null;
    }


  //=====================================================================
  //  conformsTo
  //=====================================================================
  /**
   *  Checks if result of this function conforms to Value 'v'.
   *  Used by 'showConformable' in 'Tables'.
   *
   *  @param  v the Value to be checked against.
   *  @return true if this Entity conforms to v, false otherwise.
   */
  boolean conformsTo(final Value v)
    {
      //---------------------------------------------------------------
      //  Dimension of the result is the same as that
      //  of argument to the function's inverse.
      //  Treat null as "unknown".
      //---------------------------------------------------------------
      if (inverse.dimen==null) return false;
      try
      {
        Value thisvalue = Value.parse(inverse.dimen);
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
    String param; // Parameter
    String def;   // Definition
    String dimen; // Dimension of parameter

    //===================================================================
    //  Constructor
    //===================================================================
    /**
     *  Constructs a FuncDef object.
     *
     *  @param param parameter
     *  @param def   function definition
     *  @param dimen dimension of the paremeter
     */
    FuncDef(String param, String def, String dimen)
      {
        this.param = param;
        this.def = def;
        this.dimen = dimen;
      }

    //===================================================================
    //  applyTo
    //===================================================================
    /**
     *  Applies the function defined by this object to a given Value,
     *  and changes the Value to the result.
     *
     *  @param v the argument and result.
     */
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
            throw new EvalError("Invalid argument dimension, "
                            + dimen + ", of function " + inv + name +
                            ". " + e.getMessage());
          }
          dim.completereduce();
          if (!dim.isCompatibleWith(v,Ignore.NONE))
          throw new EvalError("Argument " + v.asString() +
                            " of function " + inv + name +
                            " is not conformable to " +
                            dim.asString() + ".");
        }

        Value result;
        try
        { result = Value.parse(def,param,v); }
        catch (EvalError e)
        {
          throw new EvalError("Invalid application of function '" +
                           inv + name + "'. " + e.getMessage());
        }

        v.copyFrom(result);
      }

  } // end FuncDef
}
