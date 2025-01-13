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
//    050315 Changed package name to 'units'.
//
//  Version 1.87.J01
//    091031 Moved here definition of Ignore.
//
//  Version 1.88.J02
//    110403 'showdef' modified to return String instead of printing.
//
//  Version 1.89.J01
//    120202 Use a method from 'Double' instead of 'Util.strtod'
//           to decide 'isNumber' attribute in the constructor.
//    120209 Moved definition of Ignore back to separate file as enum.
//           (No longer needs to be a set of constants in a class.)
//    120303 Added check for a valid name to 'split'.
//           Substantial rewrite of 'showdef'.
//
//=========================================================================

package net.sourceforge.unitsinjava;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Factor
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  An entity that can be a factor in a Product:
 *  a unit or a prefix.
 */

abstract public class Factor extends Entity
{
  //-------------------------------------------------------------------
  /**  Definition string. */
  //-------------------------------------------------------------------
  public String def;

  //-------------------------------------------------------------------
  /** Is this a primitive unit? */
  //-------------------------------------------------------------------
  boolean isPrimitive = false;

  //-------------------------------------------------------------------
  /** Is this a dimensionless primitive unit? */
  //-------------------------------------------------------------------
  boolean isDimless = false;

  //-------------------------------------------------------------------
  /**  Is the definition a number? */
  //-------------------------------------------------------------------
  boolean isNumber = false;

  //-------------------------------------------------------------------
  /**  Ignore in comparisons? */
  //-------------------------------------------------------------------
  boolean ignoredIf(Ignore what)
    {
      if (what==Ignore.PRIMITIVE && isPrimitive) return true;
      if (what==Ignore.DIMLESS   && isDimless) return true;
      return false;
    }


  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs a Factor object.
   *
   *  @param name name of the Factor.
   *  @param loc  location where defined.
   *  @param def  definition.
   */
  Factor(final String name, Location loc, final String def)
    {
      super(name,loc);
      this.def = def;

      if (def.equals("!"))
        isPrimitive = true;

      if (def.equals("!dimensionless"))
      {
        isPrimitive = true;
        isDimless = true;
      }

      Double d;
      try
        { d = Double.valueOf(def); }
      catch(NumberFormatException e)
        { return; }

      if (d.isInfinite() || d.isNaN())
        return;

      isNumber = true;
    }

  //=====================================================================
  //  split
  //=====================================================================
  /**
   *  Finds out if given string is the name of a unit or prefix,
   *  or is a prefixed unit name. The unit name given as 'name'
   *  or its part may be in plural.
   *  (Originally part of 'lookupunit'.)
   *
   *  @param  name string to be investigated.
   *  @return two-element array where first element is the Prefix
   *          (or null if none) and second is the Unit (or null if none).
   *          <br>
   *          Null if the name is not recognized.
   */
  public static Factor[] split(final String name)
    {
      //---------------------------------------------------------------
      //  Return null if 'name' is not a valid name.
      //---------------------------------------------------------------
      if (Entity.checkName(name)!=null) return null;

      //---------------------------------------------------------------
      //  If 'name' is a unit name, possibly in plural form,
      //  return its Unit object.
      //---------------------------------------------------------------
      Unit u = Unit.find(name);
      if (u!=null)
        return new Factor[]{null,u};

      //---------------------------------------------------------------
      //  The 'name' is not a unit name.
      //  See if it is a prefix or prefixed unit name.
      //---------------------------------------------------------------
      Prefix p = Prefix.find(name);

      //---------------------------------------------------------------
      //  Return null if not a prefix or prefixed unit name.
      //---------------------------------------------------------------
      if (p==null)
        return null;

      //---------------------------------------------------------------
      //  Get the prefix string.
      //  If it is all of 'name', return its Prefix object.
      //---------------------------------------------------------------
      String prefix = p.name;
      if (name.equals(prefix))
        return new Factor[]{p,null};

      //---------------------------------------------------------------
      //  The 'name' has a known prefix 'prefix'.
      //  Split 'name' into  the prefix and 'rest'.
      //  If 'rest' (or its singular form) is a unit name,
      //  return the Prefix and Unit objects.
      //---------------------------------------------------------------
      String rest = name.substring(prefix.length(),name.length());
      u = Unit.find(rest);
      if (u!=null)
        return new Factor[]{p,u};

      //---------------------------------------------------------------
      //  Return null if 'rest' is not a unit name.
      //---------------------------------------------------------------
      return null;
    }


  //=====================================================================
  //  showdef
  //=====================================================================
  /**
   *  If given string is the name of a unit or prefix, or is a prefixed
   *  unit name, return its definition. Otherwise return null.
   *  (Modified part of 'showdefinition'.)
   *
   *  @param  name string to be investigated.
   *  @return definition of 'name' or null.
   */
  public static String showdef(final String name)
    {
      StringBuilder sb = new StringBuilder();

      String def = name;

      //---------------------------------------------------------------
      //  This loop produces possibly in 'sb' a chain of definitions
      //  preceded by equal sign. It ends either by 'return' that
      //  delivers the definition, or by 'break' that proceeds to add
      //  the reduced form of the Value represented by 'name'.
      //  These exits are commented below.
      //---------------------------------------------------------------
      while(true)
      {
        //-------------------------------------------------------------
        //  Split 'def' into prefix and unit - if possible.
        //-------------------------------------------------------------
        Factor[] pu = split(def);

        //-------------------------------------------------------------
        //  If 'def' is not a prefix, unit, or prefix-unit combination,
        //  proceed to append the reduced form of 'name'.
        //-------------------------------------------------------------
        if (pu==null) break;

        Factor pref = pu[0];
        Factor unit = pu[1];

        //-------------------------------------------------------------
        //  If 'def' is a stand-alone prefix defined as a number,
        //  append this definition to 'sb' and return result.
        //  If it is not defined as a number, append the definition
        //  to 'sb', and repeat the process for that definition.
        //-------------------------------------------------------------
        if (unit==null)
        {
          def = pref.def;
          if (pref.isNumber)
            return name + sb.toString() + " = " + def;

          sb.append(" = " + def);
          continue;
        }

        //-------------------------------------------------------------
        //  If 'def' is a unit defined as a number, append
        //  this definition to 'sb' and return result.
        //  If 'def' is a primitive unit return either a text stating
        //  that, or the definition chain accumulated in 'sb'.
        //  If 'def' is any other unit, append its definition to 'sb',
        //  and repeat the process for that definition.
        //-------------------------------------------------------------
        if (pref==null)
        {
          def = unit.def;
          if (unit.isNumber)
            return name + sb.toString() + " = " + def;
          else if (unit.isPrimitive)
          {
            if (sb.length()==0) return "'" + name + "' is a primitive unit";
            else return name + sb.toString();
          }

          sb.append(" = " + def);
          continue;
        }

        //-------------------------------------------------------------
        //  If 'def' is a prefix-unit combination, append combined
        //  definition to 'sb', and proceed to append the reduced
        //  form of 'name'.
        //-------------------------------------------------------------
        sb.append(" = " + pref.def + " " +
              (unit.isPrimitive || unit.isNumber? unit.name : unit.def));
        break;
      }

      //---------------------------------------------------------------
      //  Append and return the reduced form of name - or return null
      //  if 'name' cannot be evaluated.
      //---------------------------------------------------------------
      Value v;
      try
      { v = Value.parse(name); }
      catch(EvalError e)
      {return null; }

      v.completereduce();
      return name + sb.toString() + " = " + v.asString();
    }
}
