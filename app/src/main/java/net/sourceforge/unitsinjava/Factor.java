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
//    091031 Version 1.97.J01. Moved here definition of Ignore
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

  public enum Ignore {NONE, PRIMITIVE, DIMLESS};

  //=====================================================================
  //  Construct object for factor 'nam' appearing at 'loc'.
  //  The factor is defined by string 'df'.
  //=====================================================================
  Factor(final String nam, Location loc, final String df)
    {
      super(nam,loc);
      def = df;

      if (df.equals("!"))
        isPrimitive = true;

      if (df.equals("!dimensionless"))
      {
        isPrimitive = true;
        isDimless = true;
      }

      isNumber = Util.strtod(def,0)==def.length();
    }

  //=====================================================================
  //  Find out if 'name' is the name of a unit or prefix, or is a prefixed
  //  unit name. The unit name given as 'name' or its part may be in plural.
  //  Return a two-element array where first element is the Prefix
  //  (or null if none) and second is the Unit (or null if none).
  //  Return null if 'name' is not recognized.
  //  (Originally part of 'lookupunit'.)
  //=====================================================================
  public static Factor[] split(final String name)
    {
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
  //  If 'name' is the name of a unit or prefix, or is a prefixed
  //  unit name, print its definition followed by equal sign.
  //  Repeat this for the definition thus obtained.
  //  (Originally part of 'showdefinition'.)
  //=====================================================================
  public static void showdef(final String name)
    {
      String def = name;

      while(true)
      {
        Factor[] pu = split(def);

        if (pu==null) break; // Not a prefix-unit

        Factor pref = pu[0];
        Factor unit = pu[1];

        if (unit==null)      // Prefix only
        {
          if (pref.isNumber) break;
          def = pref.def;
        }

        else if (pref==null) // Unit only
        {
          if (unit.isPrimitive || unit.isNumber) break;
          def = unit.def;
        }

        else                  // Prefix and unit
        {
          def = pref.def + " "
                + (unit.isPrimitive? unit.name : unit.def);
        }

        Env.out.print(def + " = ");
      }
    }
}
