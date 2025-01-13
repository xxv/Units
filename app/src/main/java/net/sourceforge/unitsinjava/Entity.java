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
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.87.J01.
//    091024 Redefined 'insertAlph' using generics.
//    091031 Replaced 'addtolist' by 'isCompatibleWith'.
//           Implemented Comparable interface.
//    091101 Removed 'insertAlph'.
//
// Version 1.89.J01.
//    120127 Added 'alias' to the comment.
//           Removed unused import.
//    120202 Added method 'checkName'.
//    120208 Renamed one-argument method 'isCompatibleWith'
//           to 'conformsTo' to avoid confusion with two-argument one
//           defined in Product and Value.
//
//=========================================================================

package net.sourceforge.unitsinjava;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Entity
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A unit, prefix, function, or alias.
 */

abstract class Entity implements Comparable<Entity>
{
  /** Name of this Entity. */
  public String name;

  /** Where this Entity is defined. */
  Location location;

  //=====================================================================
  //  Constructor.
  //=====================================================================
  /**
   *  Constructs the Entity object.
   *
   *  @param name name.
   *  @param location where defined.
   */
  Entity(final String name, final Location location)
    {
      this.name = name;
      this.location = location;
    }


  //=====================================================================
  //  compareTo
  //=====================================================================
  /**
   *  Implements comparator of the 'Comparable' interface.
   *
   *  @param  e the Entity to be compared with this Entity.
   *  @return &lt;0 if this&lt;e, 0 if this==e, &gt;0 if this&gt;e.
   */
  public int compareTo(Entity e)
    { return name.compareTo(e.name); }


  //=====================================================================
  //  check
  //=====================================================================
  /**
   *  Checks definition of this Entity for correctness.
   *  Writes diagnostics to 'Env.out'.
   *  Used by 'check' in 'Tables'.
   */
  abstract void check();


  //=====================================================================
  //  conformsTo
  //=====================================================================
  /**
   *  Checks if this Entity conforms to Value 'v'.
   *  Used by 'showConformable' in 'Tables'.
   *
   *  @param  v the Value to be checked against.
   *  @return true if this Entity conforms to v, false otherwise.
   */
  abstract boolean conformsTo(final Value v);


  //=====================================================================
  //  desc
  //=====================================================================
  /**
   *  Returns short description of this Entity
   *  to be shown by 'showConformable' and 'showMatching' in 'Tables'.
   *
   *  @return description.
   */
  abstract String desc();


  //=====================================================================
  //  checkName
  //=====================================================================
  /**
   *  Checks if 'name' is a correct Entity name.
   *
   *  @param  name name to be checked.
   *  @return diagnostic message, or null if 'name' is correct.
   */
  public static String checkName(final String name)
    {
      if ("0123456789".indexOf(name.charAt(0))>=0)
        return "begins with a digit";

      if ("_.,~".indexOf(name.charAt(0))>=0)
        return "begins with '" + name.charAt(0) + "'";

      int lg = name.length();

      if (".,_".indexOf(name.charAt(lg-1))>=0)
        return "ends with '" + name.charAt(lg-1) + "'";

      int i = Util.indexOf("+-*/|^();#",name,1);
      if (i<lg)
        return "contains '" + name.charAt(i) + "'";

      return null;
    }

}

