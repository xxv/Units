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
//    061229 Version 1.86.J01. Corrected test for 'verbose'.
//    091024 Version 1.87.J01. Used modified 'insertAlph'.
//    091031 Moved definition of Ignore to Factor.
//           Replaced 'addtolist' by 'isCompatibleWith'.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Hashtable;
import java.util.Vector;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Unit
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A unit.
 */

 public class Unit extends Factor
{
  //-------------------------------------------------------------------
  //  Table of Units
  //-------------------------------------------------------------------
  public static Hashtable<String,Unit> table = null;


  //=====================================================================
  //  Construct object for unit 'nam' defined at 'loc'.
  //  The unit is defined by string 'df'.
  //=====================================================================
  Unit(final String nam, Location loc, final String df)
    { super(nam,loc,df); }


  //=====================================================================
  //  Given is a line number 'lin' from units.dat file, parsed into
  //  name 'nam' and definition 'df'. It should be a unit definition.
  //  Construct a Unit object defined by the line, enter it into
  //  Units table, and return true.
  //=====================================================================
  public static boolean accept
    ( final String nam, final String df, Location loc)
    {
      // Units that end in [2-9] can never be accessed.

      if ("23456789".indexOf(nam.charAt(nam.length()-1))>=0)
      {
         Env.err.println
           ("Unit '" + nam + "' on line " + loc.lineNum
             +  " ignored. It ends with a digit 2-9.");
         return true;
      }

      // Units that start with a digit can never be accessed.

      if ("0123456789".indexOf(nam.charAt(0))>=0)
      {
         Env.err.println
           ("Unit '" + nam + "' on line " + loc.lineNum
             +  " ignored. It starts with a digit.");
         return true;
      }

      // Is it a redefinition?

      if (table.containsKey(nam))
      {
        Env.err.println
          ("Redefinition of unit '" + nam
            + "' on line " + loc.lineNum + " is ignored.");
         return true;
      }

      // Install unit in table.

      table.put(nam, new Unit(nam,loc,df));
      return true;
    }


  //=====================================================================
  //  Check the unit definition. Used in 'checkunits'.
  //=====================================================================
  public static Value one = new Value();

  void check()
    {
      // check if can be reduced
      if (Env.verbose==2)
        Env.out.println("doing '" + name + "'");
      Value v = Value.fromString(name);
      if (v==null || !v.isCompatibleWith(one,Factor.Ignore.PRIMITIVE))
        Env.out.println
          ("'" + name + "' defined as '"
           + def + "' is irreducible");

      // check if not hidden by function
      if (DefinedFunction.table.containsKey(name))
        Env.out.println
          ("unit '" + name
           + "' is hidden by function '" + name + "'");
    }


  //=====================================================================
  //  Return true if this unit is compatible with Value 'v',
  //=====================================================================
  boolean isCompatibleWith(final Value v)
    {
      Value thisvalue = Value.fromString(name);
      if (thisvalue==null) return false;
      return thisvalue.isCompatibleWith(v,Factor.Ignore.DIMLESS);
    }


  //=====================================================================
  //  Return short description of this object to be shown by 'tryallunits'.
  //=====================================================================
  String desc()
    { return (isPrimitive? "<primitive unit>" : "= " + def); }


  //=====================================================================
  //  Find out if 'name' is the name of a known unit, possibly in plural.
  //  Return the Unit object if so, or null otherwise.
  //  (Originally part of 'lookupunit'.)
  //=====================================================================
  public static Unit find(final String name)
    {
      //---------------------------------------------------------------
      //  If 'name' appears as unit name in table,
      //  return object from the table.
      //---------------------------------------------------------------
      if (Unit.table.containsKey(name))
        return Unit.table.get(name);

      //---------------------------------------------------------------
      //  Plural rules for English: add -s
      //  after x, sh, ch, ss   add -es
      //  -y becomes -ies except after a vowel when you just add -s
      //  Try removing 's'.
      //---------------------------------------------------------------
      int ulg = name.length();
      if (ulg>2 && name.charAt(ulg-1)=='s')
      {
        String temp = name.substring(0,ulg-1);
        if (Unit.table.containsKey(temp))
          return Unit.table.get(temp);

        //-------------------------------------------------------------
        //  Removing the suffix 's' did not help. It could still be
        //  a plural form ending on 'es'. Try this.
        //-------------------------------------------------------------
        if (ulg>3 && name.charAt(ulg-2)=='e')
        {
          temp = name.substring(0,ulg-2);
          if (Unit.table.containsKey(temp))
            return Unit.table.get(temp);

          //-----------------------------------------------------------
          //  Removing the suffix 'es' did not help. It could still be
          //  a plural form ending on 'ies'. Try this.
          //-----------------------------------------------------------
          if (ulg>4 && name.charAt(ulg-3)=='i')
          {
            temp = name.substring(0,ulg-3) + "y";
            if (Unit.table.containsKey(temp))
              return Unit.table.get(temp);
          }
        }
      }

      return null;
    }
}
