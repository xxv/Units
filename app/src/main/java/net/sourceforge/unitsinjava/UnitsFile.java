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
//  Version 1.84.J05.
//    050203 MAXINCLUDE moved to Env object.
//
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.89.J01.
//    120121 Changed name from 'File' to 'UnitsFile'
//           to eliminate conflicts with Java class 'File'.
//    120123 Modified to read the file as UTF8-encoded.
//           Modified 'open' in 'StandAcc' and 'AppletAcc'
//           to return InputStream instead of BufferedReader.
//           Append name of include file to 'Env.filenames'.
//    120201 Restructured to use File Parser.
//    120326 'FileAcc' moved from Env to UntisFile.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.net.URL;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class UnitsFile
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Represents a unit definition file.
 */

 public class UnitsFile
{
  //-------------------------------------------------------------------
  //  File access method.
  //-------------------------------------------------------------------
  public static FileAcc fileAcc;

  //-------------------------------------------------------------------
  //  File name.
  //-------------------------------------------------------------------
  String name;

  //-------------------------------------------------------------------
  //  String-mapped contents of the file.
  //  It is used to show source in browser window.
  //-------------------------------------------------------------------
  String contents;

  //-------------------------------------------------------------------
  //  Character encoding assumed for the file.
  //-------------------------------------------------------------------
  private static final Charset cs = Charset.forName("UTF-8");


  //=====================================================================
  //  Constructor
  //=====================================================================
  /**
   *  Constructs UnitsFile object for file with specified name.
   *  Does not find or open the file.
   *
   *  @param  name file name
   */
  UnitsFile(final String name)
    { this.name = name; }


  //=====================================================================
  //  readunits
  //=====================================================================
  /**
   *  Reads definitions from the file.
   *  Creates objects for the Entities thus defined.
   *  Saves String-mapped copy of the file in 'contents'.
   *  Returns 'false' if the file was not found
   *  or reading error occurred.
   *
   *  @param  depth include depth.
   *  @return true if file successfully processed; false otherwise.
   */
  boolean readunits(int depth)
    {
      //---------------------------------------------------------------
      //  Current line number. Line numbers start with 1.
      //---------------------------------------------------------------
      int linenum = 1;

      //---------------------------------------------------------------
      //  Line number of a complete definition.
      //  If the definition extends into continuation lines,
      //  it is number of the first line.
      //---------------------------------------------------------------
      int linestart = 0;

      //---------------------------------------------------------------
      //  Positions in file. They are indices in 'contents'.
      //---------------------------------------------------------------
      int pos = 0;        // Position in file
      int startpos = 0;   // Starting position of current line

      //---------------------------------------------------------------
      //  Buffer to accumulate the contents.
      //---------------------------------------------------------------
      StringBuilder fileBuf = new StringBuilder();

      //---------------------------------------------------------------
      //  Open the file.
      //---------------------------------------------------------------
      InputStream is = fileAcc.open(name);
      if (is==null) return false;
      InputStreamReader isr = new InputStreamReader(is,cs);
      BufferedReader reader = new BufferedReader(isr);

      //---------------------------------------------------------------
      //  Instantiate parser with memoization = 1.
      //---------------------------------------------------------------
      FileParser parser = new FileParser();
      FileSemantics sem = parser.semantics();
      parser.setMemo(1);

      //---------------------------------------------------------------
      //  Process definitions.
      //---------------------------------------------------------------
      processLoop:
      while(true)
      {
        //-------------------------------------------------------------
        //  Get one complete definition line into 'lineBuf'.
        //  It may include continuation lines.
        //-------------------------------------------------------------
        StringBuilder lineBuf = new StringBuilder();

        linestart = linenum; // Note starting line number
        startpos = pos;      // ..and position in file.

        boolean continued = false;

        lineLoop:
        while (true)
        {
          String line;

          //-----------------------------------------------------------
          //  Get line from file, exit 'readline' on error.
          //-----------------------------------------------------------
          try
            { line = reader.readLine(); }
          catch (IOException e)
            {
              Env.out.println
                ("Error in reading line " + linenum +
                 " from file '" + name +"': " + e.getMessage());
              return false;
            }

          //-----------------------------------------------------------
          //  End of file reached. Exit 'processLoop'.
          //-----------------------------------------------------------
          if (line==null)
          {
            if (continued) // If continuation was expected
              Env.out.println
                ("The last line of '" + name +
                 "' is missing its continuation and is ignored.");
            break processLoop;
          }

          //-----------------------------------------------------------
          //  We have a line. Append it to 'fileBuf' with '\n' added.
          //-----------------------------------------------------------
          fileBuf.append(line).append("\n");
          pos += (line.length()+1); // Current position is after '\n'.
          linenum++;                // Next line number.

          //-----------------------------------------------------------
          //  Append the line to 'lineBuf'.
          //-----------------------------------------------------------
          lineBuf.append(line);

          //-----------------------------------------------------------
          //  If the line ends with '\', replace '\' with a blank
          //  in 'lineBuf' and note that continuation is expected.
          //-----------------------------------------------------------
          if (line.endsWith("\\"))
          {
            lineBuf.setCharAt(lineBuf.length()-1,' ');
            continued = true;
          }
          //-----------------------------------------------------------
          //  Otherwise exit lineLoop.
          //-----------------------------------------------------------
          else
            break lineLoop;

        } // end lineLoop

        //-------------------------------------------------------------
        //  StringBuilder 'lineBuf' contains now a complete line.
        //  Get rid of the comment, if any.
        //-------------------------------------------------------------
        int i = lineBuf.indexOf("#");
        if (i>=0) lineBuf.delete(i,lineBuf.length());
        String line = lineBuf.toString();

        //-------------------------------------------------------------
        //  Process the line.
        //-------------------------------------------------------------
        SourceString src = new SourceString(line);
        sem.loc = new Location(this,linestart,startpos,pos-1);
        sem.depth = depth;
        parser.parse(src);

      } // end processLoop

      //---------------------------------------------------------------
      //  Close the file.
      //---------------------------------------------------------------
      try
        { reader.close(); }
      catch (IOException e)
        { Env.out.println(e.getMessage()); }

      //---------------------------------------------------------------
      //  Save file contents.
      //---------------------------------------------------------------
      contents = fileBuf.toString();

      return true;
    }


  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Class FileAcc
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  /**
   *  Access to files.
   *  Different subclasses of FileAcc specify different ways to open
   *  the unit definition files in different environments.
   *  An object of the proper subclass for current environment
   *  is instantiated and assigned to 'fileAcc'.
   *  <ul>
   *  <li>UnitsWindow and convert plugs an instance of StandAcc into 'fileAcc'.
   *  <li>applet plugs an instance of AppletAcc into 'fileAcc'.
   *  </ul>
   */
  abstract public static class FileAcc
  { public abstract InputStream open(final String name); }
}