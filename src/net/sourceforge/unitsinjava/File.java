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
//    050203 Version 1.84.J05. MAXINCLUDE moved to Env object.
//    050315 Version 1.84.J07. Changed package name to "units".
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.FileNotFoundException;

import java.net.URL;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class File
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A unit definition file.
 */

 public class File
{
  String name;
  String contents;



  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs File object for file with specified name.
   *  Does not find or open the file.
   *
   *  @param  n file name
   */
  File(final String n)
    { name = n; }


  //=====================================================================
  //  readunits
  //=====================================================================
  /**
   *  Reads definitions from the file.
   *  Creates objects for the Entities thus defined.
   *  Saves contents of the file.
   *
   *  @param  depth include depth.
   *  @return true if file successfully processed; false otherwise.
   */
  boolean readunits(int depth)
    {
      final String WHITE = " \t";  // Whitespace characters

      //---------------------------------------------------------------
      //  Open the file.
      //---------------------------------------------------------------
      BufferedReader reader = Env.files.open(name);
      if (reader==null)
      {
        Env.err.println("File '" + name + "' not found.");
        return false;
      }

      //---------------------------------------------------------------
      //  Line numbers.
      //---------------------------------------------------------------
      int linenum = 0;    // Current line number
      int linestart = 0;  // Number of line being continued
      int pos = 0;
      int startpos = 0;

      //---------------------------------------------------------------
      //  Indicator: if true we are currently reading data
      //  for the wrong locale so we should skip it.
      //---------------------------------------------------------------
      boolean wronglocale = false;

      //---------------------------------------------------------------
      //  Indicator: if true we are currently reading data
      //  for some locale (right or wrong).
      //---------------------------------------------------------------
      boolean inlocale = false;

      //---------------------------------------------------------------
      //  Buffer to accumulate the contents.
      //---------------------------------------------------------------
      StringBuffer fb = new StringBuffer();

      //---------------------------------------------------------------
      //  Reading loop.
      //---------------------------------------------------------------
      readLoop:
      while(true)
      {
        //-------------------------------------------------------------
        //  Get one complete line (with continuations).
        //-------------------------------------------------------------
        linestart = linenum;
        startpos = pos;
        String line;
        StringBuffer sb = new StringBuffer();
        boolean haveLine = false;
        while (!haveLine)
        {
          try
            { line = reader.readLine(); }
          catch (IOException e)
            {
              Env.err.println(e.toString());
              return false;
            }
          if (line==null) // End of file
            {
              if (sb.length()==0) // Not looking for continuation
                break readLoop;
              Env.err.println("Missing continuation to last line of " +
                                 name + ".");
              return false;
            }
          fb.append(line).append("\n");
          linenum++;
          pos += (line.length()+1);

          if (line.endsWith("\\"))
            sb.append(line.substring(0,line.length()-1).trim()).append(" ");
          else
          {
            sb.append(line.trim());
            haveLine = true;
          }
        }

        //-------------------------------------------------------------
        //  StringBuffer sb contains now a complete line.
        //-------------------------------------------------------------
        line = sb.toString();

        //-------------------------------------------------------------
        //  Remove comment at the end of line (if any).
        //-------------------------------------------------------------
        int cmt = line.indexOf('#');
        if (cmt>=0) line = line.substring(0,cmt).trim();

        //-------------------------------------------------------------
        //  Skip empty line.
        //-------------------------------------------------------------
        if (line.length()==0) continue;

        //-------------------------------------------------------------
        //  If line is a units.dat command, process it.
        //-------------------------------------------------------------
        if (line.charAt(0)=='!')
        {
          int i = Util.indexOf(WHITE,line,1);
          String command = line.substring(1,i);

          //-----------------------------------------------------------
          //  Process '!locale'
          //-----------------------------------------------------------
          if (command.equals("locale"))
          {
            String argument = line.substring(i,line.length()).trim();
            i = Util.indexOf(WHITE,argument,0);
            argument = argument.substring(0,i);

            if (argument.length()==0)
            {
              Env.out.println
                ("No locale specified on line " + linenum + " of '"
                 + name + "'.");
              continue;
            }

            if (inlocale)
            {
              Env.out.println
                ("Nested locales not allowed, line " + linenum + " of '"
                 + name + "'.");
              continue;
            }

            inlocale = true;
            if (!argument.equals(Env.locale))
              wronglocale = true;
            continue;
          }

          //-----------------------------------------------------------
          //  Process '!endlocale'
          //-----------------------------------------------------------
          if (command.equals("endlocale"))
          {
            if (!inlocale)
            {
              Env.out.println
                ("Unmatched !endlocale on line " + linenum + " of '"
                 + name + "'.");
              continue;
            }

            inlocale = false;
            wronglocale = false;
            continue;
          }

          //-----------------------------------------------------------
          //  Process '!include', but only in right locale.
          //-----------------------------------------------------------
          if (command.equals("include"))
          {
            if (wronglocale) continue;

            String argument = line.substring(i,line.length()).trim();
            i = Util.indexOf(WHITE,argument,0);
            argument = argument.substring(0,i);

            if (depth>=Env.MAXINCLUDE)
            {
              Env.out.println
                ("Max include depth of " + Env.MAXINCLUDE + " exceeded on line "
                 + linenum + " of '" + name + "'.");
              continue;
            }

            File infile = new File(argument);
            infile.readunits(depth+1);
            continue;
          }

          //-----------------------------------------------------------
          //  Process invalid command.
          //-----------------------------------------------------------
          Env.out.println
            ("Invalid command '!" + command + "' in line "
             + linestart + " of units file '" + name +"'.");
          continue;
        }

        //-------------------------------------------------------------
        //  Skip the line if wrong locale.
        //-------------------------------------------------------------
        if (wronglocale) continue;

        //-------------------------------------------------------------
        //  The line is definition of unit, prefix, function, or table.
        //  Split it into name and definition.
        //-------------------------------------------------------------
        int i = Util.indexOf(WHITE,line,0);
        String unitname = line.substring(0,i).trim();
        String unitdef = line.substring(i,line.length()).trim();
        if (unitdef.length()==0)
        {
          Env.out.println
            ("Missing definition in line " + linestart
             + " of units file '" + name +"'.");
          continue;
        }

        //-------------------------------------------------------------
        //  Enter definition from the line into tables.
        //-------------------------------------------------------------
        Location loc = new Location(this,linestart,startpos,pos-1);

        //  If line is a prefix definition:

        if (Prefix.accept(unitname,unitdef,loc))
          continue;

        //  If line is a table definition:

        if (TabularFunction.accept(unitname,unitdef,loc))
          continue;

        //  If line is a function definition:

        if (ComputedFunction.accept(unitname,unitdef,loc))
          continue;

        //  Otherwise line is a unit definition.

        Unit.accept(unitname,unitdef,loc);

      } // end ReadLoop

      //---------------------------------------------------------------
      //  Close the file.
      //---------------------------------------------------------------
      try
        { reader.close(); }
      catch (IOException e)
        { Env.err.println(e.toString()); }

      //---------------------------------------------------------------
      //  Save file contents.
      //---------------------------------------------------------------
      contents = fb.toString();

      return true;
    }
}
