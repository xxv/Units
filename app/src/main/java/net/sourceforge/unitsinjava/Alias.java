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
//  Version 1.89.J01
//    120201 Created for this version.
//    120317 Changed 'define' to replace an earlier definition
//           instead of ignoring re-definition.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Hashtable;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Alias
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Alias for a UnitList.
 */

 public class Alias extends Entity
{
  //-------------------------------------------------------------------
  //  Unit list that the alias stands for.
  //-------------------------------------------------------------------
  final String unitList;

  //-------------------------------------------------------------------
  //  Table of Aliases.
  //-------------------------------------------------------------------
  public static Hashtable<String,Alias> table = null;


  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs an Alias object.
   *
   *  @param  name  the alias.
   *  @param  ulist unit list that the alias stands for.
   *  @param  loc   location where defined.
   */
  Alias(final String name, final String ulist, final Location loc)
    {
      super(name,loc);
      unitList = ulist.replace(" ","").replace("\t","");
    }


  //=====================================================================
  //  define
  //=====================================================================
  /**
   *  Constructs an Alias object from a parsed '!unitlist' statement.
   *  Checks the name and, if correct, enters the object into Alias table.
   *  Writes diagnostics to Env.out.
   *
   *  @param  name  the alias.
   *  @param  ulist the unit list.
   *  @param  loc   location where defined.
   */
  public static void define
    ( final String name, final String ulist, final Location loc)
    {
      //---------------------------------------------------------------
      //  Alias with incorrect name can never be accessed.
      //---------------------------------------------------------------
      String diag = Entity.checkName(name);

      if (diag!=null)
      {
         Env.out.println
           (loc.where() + ". Alias '" + name
            + "' is ignored. It " + diag + ".");
         return;
      }

      //---------------------------------------------------------------
      //  Install the alias in table.
      //---------------------------------------------------------------
      Alias old = table.put(name, new Alias(name,ulist,loc ));

      //---------------------------------------------------------------
      //  Write a message if an earlier definition was replaced.
      //---------------------------------------------------------------
      if (old!=null)
      {
        Env.out.println
          ("Unit list '" + name + "' defined in " + old.location.where() +
           ", is redefined in " + loc.where() + ".");
      }
    }


  //=====================================================================
  //  check
  //=====================================================================
  /**
   *  Checks definition of this Alias for correctness.
   *  Writes diagnostics to 'Env.out'.
   *  Used by 'check' in 'Tables'.
   */
  void check()
    {
      //---------------------------------------------------------------
      //  If requested, write check trace.
      //---------------------------------------------------------------
      String where = location.where() + ". ";
      if (Env.verbose==2)
        Env.out.println(where + "Doing '" + name + "'.");

      //---------------------------------------------------------------
      //  Check the unit list for correctness.
      //---------------------------------------------------------------
      try
      { UnitList ul = new UnitList(unitList); }
      catch(EvalError e)
      {
        Env.out.println
            (where + "Unit list '" + name + "'. " + e.getMessage());
      }

      //---------------------------------------------------------------
      //  Alias must be different from function, unit, and prefix.
      //---------------------------------------------------------------
      Function func = DefinedFunction.table.get(name);
      if (func!=null)
        Env.out.println
          (where + "Unit list '" + name +
           "' hides the function defined in " +
           func.location.where() + ".");

      Unit unit = Unit.table.get(name);
      if (unit!=null)
        Env.out.println
          (where + "Unit list '" + name +
           "' hides the unit defined in " +
           unit.location.where() + ".");

      Prefix pref = Prefix.table.get(name);
      if (pref!=null)
        Env.out.println
          (where + "Unit list '" + name +
           "' hides the prefix defined in " +
           pref.location.where() + ".");
    }


  //=====================================================================
  //  conformsTo
  //=====================================================================
  /**
   *  Checks if unit list defined by this Alias conforms to given Value.
   *  Fails without a message if the unit list is invalid.
   *  Used by 'showConformable' in 'Tables'.
   *
   *  @param  v the Value to be checked against.
   *  @return true if this Alias conforms to v, false otherwise.
   */
  boolean conformsTo(final Value v)
    {
      UnitList ul;
      try { ul = new UnitList(unitList); }
      catch(EvalError e) { return false; }
      return ul.value[0].isCompatibleWith(v,Ignore.DIMLESS);
    }


  //=====================================================================
  //  showdef
  //=====================================================================
  /**
   *  If the argument is the name of a unit list, returns its definition.
   *  Otherwise returns null.
   *
   *  @param  name the name to be checked.
   *  @return the definition or null.
   */
  public static String showdef(final String name)
    {
      Alias alias = Alias.table.get(name);
      if (alias!=null) return "unit list, " + alias.unitList;
      else return null;
    }


  //=====================================================================
  //  desc
  //=====================================================================
  /**
   *  Returns short description of this Alias.
   *  To be shown by 'showConformable' and 'showMatching' in 'Tables'.
   *
   *  @return description.
   */
  String desc()
    { return ("= " + unitList); }
}
