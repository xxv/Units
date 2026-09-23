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
//    050203 Do not initialize table.
//    050315 Version 1.84.J07. Changed package name to "units".
//    061229 Version 1.86.J01. Corrected test for 'verbose'.
//    091024 Version 1.87.J01.
//           Used generics for 'table'.
//    091031 Moved definition of Ignore to Factor.
//           Replaced 'addtolist' by 'isCompatibleWith'.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Hashtable;
import java.util.Vector;


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


  //=====================================================================
  //  Construct object for prefix 'nam' defined at 'loc'.
  //  The prefix is defined by string 'df'.
  //=====================================================================
  Prefix(final String nam, Location loc, final String df)
    { super(nam,loc,df); }


  //=====================================================================
  //  Given is a line from units.dat file, parsed into
  //  name 'nam' and definition 'df'. If this line defines a prefix,
  //  construct a Prefix object defined by it, enter it into Prefix table,
  //  and return true. Otherwise return false.
  //=====================================================================
  public static boolean accept
    ( final String nam, final String df, Location loc)
    {
      //  If unitname ends with '-', we have a prefix definition.

      if (!nam.endsWith("-")) return false;

      String prefname = nam.substring(0,nam.length()-1);

      // Is it redefinition?

      if (table.containsKey(prefname))
      {
        Env.err.println
          ("Redefinition of prefix '" + prefname +
           "-' on line " + loc.lineNum + " is ignored.");
        return true;
      }

      // Install prefix in table.

      table.put(prefname, new Prefix(prefname,loc,df));
      return true;
    }


  //=====================================================================
  //  Check the prefix definition. Used in 'checkunits'.
  //=====================================================================
  public static Value one = new Value();

  void check()
    {
      // check for bad '/' character in prefix
      int plevel = 0;
      for (int i=0;i<def.length();i++)
      {
        int ch = def.charAt(i);
        if (ch==')') plevel--;
        else if (ch=='(') plevel++;
        else if (plevel==0 && ch=='/')
        {
          Env.out.println
           ("'" + name + "-' defined as '"
             + def + "' contains bad '/'");
          return;
        }
      }

      // check if can be reduced
      if (Env.verbose==2)
        Env.out.println("doing '" + name + "'");
      Value v = Value.fromString(name);
      if (v==null || !v.isCompatibleWith(one,Factor.Ignore.PRIMITIVE))
        Env.out.println
          ("'" + name + "' defined as '"
           + def + "' is irreducible");
    }


  //=====================================================================
  //  These methods, defined in Entity class,
  //  are never called for a Prefix object.
  //=====================================================================
  boolean isCompatibleWith(final Value v)
    { throw new Error("Program Error"); }

  String desc()
    { throw new Error("Program Error"); }


  //=====================================================================
  //  Find the longest prefix of 'name' that is in Prefix table,
  //  and return that Prefix object. (The prefix may all of 'name'.)
  //  Return null if name does not have a known prefix.
  //=====================================================================
  public static Prefix find(final String name)
    {
      int nlg = name.length();
      int plg;
      for (plg=nlg;plg>0;plg--)
      {
        Prefix p = table.get(name.substring(0,plg));
        if (p!=null) return p;
      }
      return null;
    }
}
