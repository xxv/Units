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
//    050203 Do not initialize table.
//
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.86.J01.
//    061229 Corrected test for 'verbose'.
//
//  Version 1.87.J01.
//    091024 Used generics for 'table'.
//    091031 Moved definition of Ignore to Factor.
//           Replaced 'addtolist' by 'isCompatibleWith'.
//
//  Version 1.89.J01
//    120201 Adapted to use with File Parser:
//           removed method 'accept' and added 'define'.
//    120208 Renamed one-argument method 'isCompatibleWith'
//           to 'conformsTo' to avoid confusion with two-argument one
//           defined in Product and Value.
//    120209 Definition of Ignore moved to separate file:
//           replaced 'Factor.Ignore' by 'Ignore'.
//    120313 Added 'location.where' to messages from 'check'.
//    120316 Added methods 'desc' and 'conformsTo'.
//    120317 Changed 'define' to replace an earlier definition
//           instead of ignoring re-definition.
//    120405 Used method 'startsWith' in 'find'.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Hashtable;
import java.util.Enumeration;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Prefix
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A prefix.
 */

 public class Prefix extends Factor
{
  //-------------------------------------------------------------------
  //  Table of Prefixes
  //-------------------------------------------------------------------
  public static Hashtable<String,Prefix> table = null;

  //-------------------------------------------------------------------
  //  Uned in conformability checks
  //-------------------------------------------------------------------
  public static Value one = new Value();


  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs a Prefix object.
   *
   *  @param name the prefix.
   *  @param loc  location where defined.
   *  @param def  definition.
   */
  Prefix(final String name, Location loc, final String def)
    { super(name,loc,def); }


  //=====================================================================
  //  define
  //=====================================================================
  /**
   *  Builds PrefixTable entry from a parsed definition.
   *  The definition is parsed as follows:
   *  <pre>
   *    name-  definition
   *  </pre>
   *
   *  @param name the prefix.
   *  @param def  definition.
   *  @param loc  location where defined.
   */
  public static void define(final String name, final String def, final Location loc)
    {
      //---------------------------------------------------------------
      //  Get name without suffix '-'.
      //---------------------------------------------------------------
      String prefname = name.substring(0,name.length()-1);

      //---------------------------------------------------------------
      //  Prefix with incorrect syntax can never be accessed.
      //---------------------------------------------------------------
      String diag = Entity.checkName(prefname);

      if (diag!=null)
      {
         Env.out.println
           (loc.where() + ". Prefix '" + prefname
            + "' is ignored. It " + diag + ".");
         return;
      }

      //---------------------------------------------------------------
      //  Install the prefix in table.
      //---------------------------------------------------------------
      Prefix old = table.put(prefname, new Prefix(prefname,loc,def));

      //---------------------------------------------------------------
      //  Write a message if an earlier definition was replaced.
      //---------------------------------------------------------------
      if (old!=null)
      {
        Env.out.println
          ("Prefix '" + name + "' defined in " + old.location.where() +
           ", is redefined in " + loc.where() + ".");
      }
    }


  //=====================================================================
  //  check
  //=====================================================================
  /**
   *  Checks definition of this Prefix for correctness.
   *  Writes diagnostics to 'Env.out'.
   *  Used by 'check' in 'Tables'.
   */
  void check()
    {
      if (Env.verbose==2)
        Env.out.println(location.where() + ". Doing '" + name + "'");

      //---------------------------------------------------------------
      // check for bad '/' character in prefix
      //---------------------------------------------------------------
      int plevel = 0;
      for (int i=0;i<def.length();i++)
      {
        int ch = def.charAt(i);
        if (ch==')') plevel--;
        else if (ch=='(') plevel++;
        else if (plevel==0 && ch=='/')
        {
          Env.out.println
           (location.where() + ". Prefix '" + name + "-' defined as '"
             + def + "' contains bad '/'");
          return;
        }
      }

      //---------------------------------------------------------------
      // check if can be reduced
      //---------------------------------------------------------------
      Value v = Value.fromString(name);
      if (v==null || !v.isCompatibleWith(one,Ignore.PRIMITIVE))
        Env.out.println
          (location.where() + ". Prefix '" + name + "' defined as '"
           + def + "' is irreducible");
    }


  //=====================================================================
  //  find
  //=====================================================================
  /**
   *  Finds the longest prefix of a given name that is in Prefix table.
   *  (The prefix may be all of that name.)
   *
   *  @param  name the name to be investigated.
   *  @return the longest prefix or null if none found.
   */
  public static Prefix find(final String name)
    {
      int maxlg = 0;
      Prefix maxp = null;
      for (Enumeration<Prefix> enu=Prefix.table.elements();enu.hasMoreElements();)
      {
        Prefix p = enu.nextElement();
        int plg = p.name.length();
        if (plg>maxlg && name.startsWith(p.name))
        {
          maxp = p;
          maxlg = plg;
        }
      }
      return maxp;
    }


  //=====================================================================
  //  conformsTo
  //=====================================================================
  /** Checks if this Prefix conforms to Value 'v'.
   *  Used by 'showConformable' in 'Tables'.
   *
   *  @param  v the Value to be checked against.
   *  @return true if this Prefix conforms to v, false otherwise.
   */
  boolean conformsTo(final Value v)
    { return one.isCompatibleWith(v,Ignore.DIMLESS); }


  //=====================================================================
  //  desc
  //=====================================================================
  /**
   *  Returns short description of this Prefix.
   *  To be shown by 'showConformable' and 'showMatching' in 'Tables'.
   *
   *  @return description.
   */
  String desc()
    { return ("<prefix> " + def); }
}
