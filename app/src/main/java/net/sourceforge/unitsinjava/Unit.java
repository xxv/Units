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
//  Version 1.84.J05.
//    050203 Do not initialize table.
//
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.86.J01.
//    061229 Corrected test for 'verbose'.
//
//  Version 1.87.J01.
//    091024 Used modified 'insertAlph'.
//    091031 Moved definition of Ignore to Factor.
//           Replaced 'addtolist' by 'isCompatibleWith'.
//
//  Version 1.89.J01
//    120201 Adapted to use with File Parser:
//           removed method 'accept' and added 'define'.
//    120202 Added method 'hasSubscript' and used it to check the name.
//    120208 Renamed one-argument method 'isCompatibleWith'
//           to 'conformsTo' to avoid confusion with two-argument one
//           defined in Product and Value.
//    120209 Definition of Ignore moved to separate file:
//           replaced 'Factor.Ignore' by 'Ignore'.
//    120311 Suppress error messages from 'conformsTo'.
//    120313 Added 'location.where' to messages from 'check'.
//    120317 Changed 'define' to replace an earlier definition
//           instead of ignoring re-definition.
//    120318 In 'check': removed check for name conflict.
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
  /**  Table of Units */
  //-------------------------------------------------------------------
  public static Hashtable<String,Unit> table = null;


  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs a Unit object.
   *
   *  @param name unit name.
   *  @param loc  location where defined.
   *  @param def  definition.
   */
  Unit(final String name, final Location loc, final String def)
    { super(name,loc,def); }


  //=====================================================================
  //  define
  //=====================================================================
  /**
   *  Builds UnitTable entry from a parsed definition.
   *  The definition is parsed as follows:
   *  <pre>
   *    name definition
   *  </pre>
   *
   *  @param name unit name.
   *  @param def  definition.
   *  @param loc  location where defined.
   */
  public static void define
    (final String name, final String def, final Location loc)
    {
      //---------------------------------------------------------------
      // Units with incorrect syntax can never be accessed.
      //---------------------------------------------------------------
      String diag = Entity.checkName(name);

      if (diag!=null)
      {
         Env.out.println
           (loc.where() + ". Unit '" + name
            + "' is ignored. Its name " + diag + ".");
         return;
      }

      //---------------------------------------------------------------
      // Units that end in [2-9] without '_' can never be accessed.
      //---------------------------------------------------------------
      if (!hasSubscript(name)
          && "23456789".indexOf(name.charAt(name.length()-1))>=0)
      {
         Env.out.println
           (loc.where() + ". Unit '" + name
            + "' is ignored. Its name ends with a digit 2-9 without '_'.");
         return;
      }

      //---------------------------------------------------------------
      //  Install the unit in table.
      //---------------------------------------------------------------
      Unit old = table.put(name, new Unit(name,loc,def));

      //---------------------------------------------------------------
      //  Write a message if an earlier definition was replaced.
      //---------------------------------------------------------------
      if (old!=null)
      {
        Env.out.println
          ("Unit '" + name + "' defined in " + old.location.where() +
           ", is redefined in " + loc.where() + ".");
      }
    }


  //=====================================================================
  //  check
  //=====================================================================
  /**
   *  Checks definition of this unit for correctness.
   *  Writes diagnostics to 'Env.out'.
   *  Used by 'check' in 'Tables'.
   */
  public static Value one = new Value();

  void check()
    {
      if (Env.verbose==2)
        Env.out.println(location.where() + ". Doing '" + name + "'");

      //---------------------------------------------------------------
      // check if can be reduced
      //---------------------------------------------------------------
      Value v = null;
      try
      {
        v = Value.parse(name);
        v.completereduce();
      }
      catch (EvalError e)
      {
        Env.out.println(location.where() + ". " + e.getMessage());
        return;
      }

      if (!v.isCompatibleWith(one,Ignore.PRIMITIVE))
        Env.out.println
          (location.where() + ". Unit '" + name + "' defined as '"
           + def + "' is irreducible.");
    }


  //=====================================================================
  //  conformsTo
  //=====================================================================
  /**
   *  Checks if this unit conforms to Value 'v'.
   *  Used by 'showConformable' in 'Tables'.
   *
   *  @param  v the Value to be checked against.
   *  @return true if this unitconforms to v, false otherwise.
   */
  boolean conformsTo(final Value v)
    {
      try
      {
        Value thisvalue = Value.parse(def);
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
   *  Returns short description of this unit
   *  to be shown by 'showConformable' and 'showMatching' in 'Tables'.
   *
   *  @return description.
   */
  String desc()
    { return (isPrimitive? "<primitive unit>" : "= " + def); }


  //=====================================================================
  //  find
  //=====================================================================
  /**
   *  Finds out if given string is the name of a known unit,
   *  possibly in plural, and returns the Unit object if so.
   *  (Originally part of 'lookupunit'.)
   *
   *  @param  name the string to be investigated.
   *  @return the Unit object identified by 'name',
   *          or null if none found.
   */
  public static Unit find(final String name)
    {
      //---------------------------------------------------------------
      //  If 'name' appears as unit name in table,
      //  return object from the table.
      //---------------------------------------------------------------
      if (Unit.table.containsKey(name))
        return Unit.table.get(name);

      //---------------------------------------------------------------
      //  Plural rules for English:
      //  add -s
      //  after x, sh, ch, ss add -es
      //  -y becomes -ies except after a vowel when you just add -s.
      //---------------------------------------------------------------
      int ulg = name.length();
      if (ulg>2 && name.charAt(ulg-1)=='s')
      {
        //-------------------------------------------------------------
        //  Try removing 's'.
        //-------------------------------------------------------------
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


  //=====================================================================
  //  hasSubscript
  //=====================================================================
  /**
   *  Chcks if given name ends with a 'subscript'.
   *  A subscript starts with '_' and is followed by a sequence
   *  of digits, point, and/or comma.
   *
   *  @param  name the name.
   *  @return true if the name ends with a subscript, false otherwise.
   */
  public static boolean hasSubscript(final String name)
  {
    int i = name.lastIndexOf('_');
    if (i<0 || i == name.length()-1) return false;
    for (int j = i+1;j<name.length();j++)
      if ("0123456789,.".indexOf(name.charAt(j))<0) return false;
    return true;
  }

}
