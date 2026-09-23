//=========================================================================
//
//  Part of units package -- a Java version of GNU Units program.
//
//  Units is a program for unit conversion originally written in C
//  by Adrian Mariano (adrian@cam.cornell.edu.).
//  Copyright (C) 1996, 1997, 1999, 2000, 2001, 2002, 2003, 2004,
//  2005, 2006, 2007, 2010 by Free Software Foundation, Inc.
//
//  Java version Copyright (C) 2003, 2004, 2005, 2006, 2007, 2008,
//  2009, 2010 by Roman R Redziejowski (roman.redz@tele2.se).
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
//           Added constants, 'unitcheck' and 'filenames'.
//    050205 Added method 'getProperties'.
//    050207 Added 'propfile'.
//    050226 Version 1.84.J06.
//           Expanded examples to help text moved from 'GUI' and 'convert'.
//    050315 Version 1.84.J07.
//           Changed package name to "units".
//           Removed 'Bug reports to.." from ABOUT.
//    050731 Version 1.85.J01.
//           Changed version numbers and copyright.
//    061228 Version 1.86.J01.
//           Changed version numbers and copyright.
//    061229 Changed 'verbose' to indicate compact / normal / verbose.
//           Removed 'terse'. Added 'oneline'.
//    070103 Added method 'showAbout' and variable 'gui'.
//    091024 Version 1.87.J01.
//           Used generics for 'filenames'.
//    091028 Changed version numbers and copyright years in 'ABOUT'
//           Added warning about obsolete currency rates.
//    091103 Added method 'getPersonalUnits'.
//    101031 Version 1.87.J01.
//           Changed version numbers and copyright years.
//           Renamed 'ABOUT' to 'COPYRIGHT' and removed version info.
//           Changed 'showAbout' to show version and invocation info
//           before copyright.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Vector;
import java.util.Properties;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class Env
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Contains static constants, variables, and methods common
 *  to different modes of invocation.
 *  Is never instantiated.
 */
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

 public class Env
{
  //-------------------------------------------------------------------
  //  Constants
  //-------------------------------------------------------------------
  public static final String PROGNAME = "gnu.units";  // Used in error messages
  public static final String VERSION = "1.88.J01";    // Program version
  public static final String ORIGVER = "1.88";        // Original version
  public static final String UNITSFILE = "units.dat"; // Default units file
  public static final String FILEVER = "1.50 (14 February 2010)";
  public static final String PROPFILE = "units.opt";  // Properties file
  public static final String DEFAULTLOCALE = "en_US"; // Default locale
  public static final int    MAXFILES = 25;           // Max number of units files
  public static final int    MAXINCLUDE = 5;          // Max depth of include files


  public static final String COPYRIGHT = ""
    + "This is an extended Java version of GNU Units " + ORIGVER + ", a program written in C\n"
    + "by Adrian Mariano, copyright (C) 1996, 1997, 1999, 2000, 2001, 2002, 2003,\n"
    + "2004, 2005, 2006, 2007, 2010 by Free Software Foundation, Inc.\n"
    + "Java version copyright (C) 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010\n"
    + "by Roman R Redziejowski.\n"
    + "The program is free software; you can redistribute it and/or modify under\n"
    + "the terms of the GNU General Public License as published by the Free Software\n"
    + "Foundation; either version 3 of the License or (at your option) any later\n"
    + "version. The program is distributed in the hope that it will be useful, but\n"
    + "WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY\n"
    + "or FITNESS FOR A PARTICULAR PURPOSE. For more details, see the GNU General\n"
    + "Public License (http://www.gnu.org/licenses/).";

  public static final String EXAMPLES =
     " Examples of conversions:\n\n"
     + " EXAMPLE 1. What is 6 feet 4 inches in meters?\n\n"
     + "   You have: 6 ft + 4 in\n"
     + "   You want: m\n"
     + "           6 ft + 4 in = 1.9304 m\n"
     + "           6 ft + 4 in = (1 / 0.51802737) m\n\n"
     + " Answer: About 1.93 m (or 1/0.518 m).\n\n"
     + " EXAMPLE 2. Thermometer shows 75 degrees Fahrenheit.\n"
     + " What is the temperature in degrees Celsius?\n\n"
     + "   You have: tempF(75)\n"
     + "   You want: tempC\n"
     + "           tempF(75) = tempC(23.88889)\n\n"
     + " Answer: About 24 C.\n\n"
     + " EXAMPLE 3. A European car maker states fuel consumption of the newest model\n"
     + " as 8 liters per 100 km. What it means in miles per gallon?\n\n"
     + "   You have: 8 liters / 100 km\n"
     + "   You want: miles per gallon\n"
     + "          reciprocal conversion\n"
     + "           1 / (8 liters / 100 km) = 29.401823 miles per gallon\n"
     + "           1 / (8 liters / 100 km) = (1 / 0.034011498) miles per gallon\n\n"
     + " Answer: About 29.4 mpg. Notice the indication that 'miles per gallon'\n"
     + " and 'liters per 100 km' are reciprocal dimensions.\n\n"
     + " EXAMPLE 4. A flow of electrons in a vacuum tube has ben measured as 5 mA.\n"
     + " How many electrons flow through the tube every second?\n"
     + " (Hint: units data file defines the electron charge as 'e'.)\n\n"
     + "   You have: 5 mA\n"
     + "   You want: e/sec\n"
     + "           5mA = 3.12075481E16 e/sec\n"
     + "           5mA = (1 / 3.2043528E-17) e/sec\n\n"
     + " Answer: About 31 200 000 000 000 000.\n\n"
     + " EXAMPLE 5. What is the energy, in electronvolts, of a photon of yellow sodium light\n"
     + " with wavelength of 5896 angstroms? (The energy is equal to Planck's constant times\n"
     + " speed of light divided by the wavelength. The units data file defines the Planck's\n"
     + " constant as 'h' and the speed of light as 'c'.)\n\n"
     + "   You have: h * (c/5896 angstroms)\n"
     + "   You want: e V\n"
     + "           h * (c/5896 angstroms) = 2.1028526 e V\n"
     + "           h * (c/5896 angstroms) = (1 / 0.4755445) e V\n\n"
     + " Answer: About 2.103 eV.\n\n";


  //-------------------------------------------------------------------
  //  Current environment
  //-------------------------------------------------------------------
  public static Vector<String> filenames;  // Unit definition files
  public static String locale;             // Locale in effect
  public static String propfile;           // Property file used

  public static int verbose;               // 0=compact, 1=normal, 2=verbose
  public static boolean quiet;             // Suppress prompting and statistics
  public static boolean oneline;           // Only one line of output
  public static boolean strict;            // Strict conversion
  public static boolean unitcheck;         // Unit checking

  public static FileAcc files;             // File system
  public static Writer out;                // Standard output
  public static Writer err;                // Standard error

  //-------------------------------------------------------------------
  //  Access to file system
  //-------------------------------------------------------------------
  abstract public static class FileAcc
  {
    public abstract BufferedReader open(final String name);
  }

  //-------------------------------------------------------------------
  //  Output writer
  //-------------------------------------------------------------------
  abstract public static class Writer
  {
    public abstract void print(final String s);
    public abstract void println(final String s);
  }


  //=====================================================================
  //  getProperties
  //=====================================================================
  /**
   *  Obtains options from properties file (if present).
   *  The file must be in the same directory as JAR.
   */
  public static void getProperties()
    {
      propfile = null; // Property file not found yet.

      //---------------------------------------------------------------
      //  For a JAR-packaged program, 'java.class.path' returned
      //  by 'System.getProperty' is a complete path of the JAR file.
      //  To obtain full path of property file, we replace the JAR name
      //  by name of the property file.
      //---------------------------------------------------------------
      String classPath = System.getProperty("java.class.path");
      String filSep = System.getProperty("file.separator");
      String propPath = classPath.substring(0,classPath.lastIndexOf(filSep)+1) + PROPFILE;

      //---------------------------------------------------------------
      //  Try to open property file. Return if not found.
      //---------------------------------------------------------------
      FileInputStream propFile;
      try
      { propFile = new FileInputStream(propPath); }
      catch (FileNotFoundException e)
      { return; }

      //---------------------------------------------------------------
      //  Read properties from the file.
      //  Write message and return if file incorrect.
      //---------------------------------------------------------------
      Properties props = new Properties();
      try
      {
        props.load(propFile);
        propFile.close();
      }
      catch (Exception e)
      {
        Env.err.println(PROGNAME + ": error reading properties from '" + propPath +"'.\n" + e);
        return;
      }

      propfile = propPath; // Property file found and read.

      //---------------------------------------------------------------
      //  If LOCALE defined, store it as Env.locale.
      //---------------------------------------------------------------
      String prop = props.getProperty("LOCALE");
      if (prop!=null) Env.locale = prop.trim();

      //---------------------------------------------------------------
      //  If UNITSFILE defined, it is a semicolon-separated list
      //  of file names. Convert it to a vector and store as Env.filenames.
      //---------------------------------------------------------------
      prop = props.getProperty("UNITSFILE");
      if (prop!=null)
      {
        Env.filenames = new Vector<String>();
        while(prop!=null)
        {
          String fileName;
          int i = prop.indexOf(';');
          if (i>=0)
          {
            fileName = prop.substring(0,i).trim();
            prop = prop.substring(i+1,prop.length());
          }
          else
          {
            fileName = prop.trim();
            prop = null;
          }

          Env.filenames.add(fileName);
        }
      }
    }


  //=====================================================================
  //  getPersonalUnits
  //=====================================================================
  /**
   *  If 'user.home' directory contains file 'units.dat',
   *  add its name (full path) to 'filenames'.
   */
  public static void getPersonalUnits()
    {
      String home = System.getProperty("user.home");
      File personal = new File(home + File.separator + "units.dat");
      if (personal.exists()) filenames.add(personal.getPath());
    }

  //=====================================================================
  //  showAbout
  //=====================================================================
  /**
   *  Write ABOUT text with information about current invocation.
   */
  public static void showAbout()
    {
      Env.out.println("Version: " + VERSION + ".");

      if (Env.propfile!=null)
        Env.out.println("Property list " + Env.propfile + ".");

      Env.out.print("Units database:");
      String sep = Env.filenames.size()==1? " ":"\n\t";
      for (int i=0;i<Env.filenames.size();i++)
      {
        String name = Env.filenames.elementAt(i);
        if (name.length()==0 || name.equals(Env.UNITSFILE))
          Env.out.print(sep + Env.UNITSFILE + " version " + Env.FILEVER);
        else Env.out.print(sep + name);
      }

      Env.out.println("\ncontaining " + Tables.stat());

      Env.out.println("Locale: " + Env.locale + ".");

      Env.out.println("\nWARNING: the currency conversions in " + UNITSFILE + " are out of date!");

      Env.out.println("\n" + Env.COPYRIGHT);
    }
}
