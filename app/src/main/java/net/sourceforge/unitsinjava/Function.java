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
//  2009 by Roman R Redziejowski (www.romanredz.se).
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
//  Version 1.87.J01.
//    091025 Replaced 'Parser.Exception' by 'EvalError'.
//
//=========================================================================

package net.sourceforge.unitsinjava;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Function
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A function. Base class for BuiltInFunction and DefinedFunction.
 */

public abstract class Function extends Entity
{
  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
    * Constructs a Function object.
    *
    *  @param name function name.
    *  @param loc  location where defined.
    */
  Function(String name,Location loc)
    { super(name,loc); }


  //=====================================================================
  //  applyTo
  //=====================================================================
  /**
   *  Applies this function to a given Value,
   *  and changes the Value to the result.
   *
   *  @param v the argument and result.
   */
  abstract void applyTo(Value v);


  //=====================================================================
  //  applyInverseTo
  //=====================================================================
  /**
   *  Applies the inverse of this function to a given Value,
   *  and changes the Value to the result.
   *
   *  @param v the argument and result.
   */
  public abstract void applyInverseTo(Value v);

  //=====================================================================
  //  showdef
  //=====================================================================
  /**
   *  Returns definition of this function.
   *  (Originally 'showfuncdef'.)
   *
   *  @return formatted definition of this function.
   */
  abstract String showdef();
}