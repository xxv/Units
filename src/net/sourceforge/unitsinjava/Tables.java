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
//    050203 Version 1.84.J05.
//           Check number of files in 'build'.
//           Initialize tables in 'build'.
//           Added method 'clean'.
//           Added title parameter to calls for Browser.
//    050315 Version 1.84.J07. Changed package name to "units".
//    061231 Version 1.86.J01.
//           Removed printing of statistics from 'build'.
//           Added method 'stat' to obtain statistics.
//    091024 Version 1.87.J01.
//           Used generics for 'Unit.table', 'Prefix.table',
//           BuiltInFunction.table', 'DefinedFunction.table', and
//           'list' in 'showConformable'.
//    091031 Replaced 'addtolist' by 'isCompatibleWith'
//           and 'insertAlph' by 'Collections.sort'.
//    091101 Implemented 'search'  ('showMatching').
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class Tables
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Contains static methods for maintenance of tables.
 */

 public class Tables
{
  //=====================================================================
  //  build
  //=====================================================================
  /**
   *  Build tables from given definition files.
   *
   *  @return true if success, false otherwise.
   */
  public static boolean build()
    {
      //---------------------------------------------------------------
      //  Check number of files.
      //---------------------------------------------------------------
      if (Env.filenames.size()>=Env.MAXFILES)
      {
        Env.out.println
          ("At most " + Env.MAXFILES + " file names are allowed.");
        return false;
      }

      //---------------------------------------------------------------
      //  Initialize all tables.
      //---------------------------------------------------------------
      if (Unit.table!=null || Prefix.table!=null ||
          BuiltInFunction.table!=null || DefinedFunction.table!=null)
        Env.err.println("Multiple invocations. Warning for interference.");

      Unit.table            = new Hashtable<String,Unit>();
      Prefix.table          = new Hashtable<String,Prefix>();
      BuiltInFunction.table = new Hashtable<String,BuiltInFunction>();
      DefinedFunction.table = new Hashtable<String,DefinedFunction>();

      //---------------------------------------------------------------
      //  Read unit definitions.
      //---------------------------------------------------------------
      for (int i=0; i<Env.filenames.size(); i++)
      {
        String filename = Env.filenames.elementAt(i);
        if (filename.length()==0) filename = Env.UNITSFILE;
        File file = new File(filename);
        boolean ok = file.readunits(0);
        if (!ok) return false;
      }

      //---------------------------------------------------------------
      //  Fill table of built-in functions.
      //---------------------------------------------------------------
      BuiltInFunction.makeTable();

      return true;
    }

  //=====================================================================
  //  clean
  //=====================================================================
  /**
   *  Remove all tables.
   */
  public static void clean()
    {
      Unit.table = null;
      Prefix.table = null;
      BuiltInFunction.table = null;
      DefinedFunction.table = null;
    }

  //=====================================================================
  //  stat
  //=====================================================================
  /**
   *  Returns string showing numbers of different entities.
   */
   public static String stat()
   {
     return Unit.table.size() + " units, " + Prefix.table.size() + " prefixes, "
            + DefinedFunction.table.size() + " nonlinear units.";
   }


  //=====================================================================
  //  check
  //=====================================================================
  /**
   *  Checks all tables.
   *  (Originally 'checkunits'.)
   *  <br>
   *  Cycles through all units and prefixes and attempts
   *  to reduce each one to 1.
   *  Prints a message for all units which do not reduce to 1.
   */
  public static void check()
    {
      //---------------------------------------------------------------
      //  Check all functions for valid definition and correct inverse.
      //---------------------------------------------------------------
      for(Enumeration e=DefinedFunction.table.elements();e.hasMoreElements();)
        ((Function)e.nextElement()).check();

      //---------------------------------------------------------------
      //  Now check all units for validity
      //---------------------------------------------------------------
      for (Enumeration e=Unit.table.elements();e.hasMoreElements();)
        ((Unit)e.nextElement()).check();


      //---------------------------------------------------------------
      //  Check prefixes
      //---------------------------------------------------------------
      for (Enumeration e=Prefix.table.elements();e.hasMoreElements();)
        ((Prefix)e.nextElement()).check();
    }


 }
