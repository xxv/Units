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
//  2009, 2010, 2011 by Roman R Redziejowski (www.romanredz.se).
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
//    050203 Added constants, 'unitcheck' and 'filenames'.
//    050205 Added method 'getProperties'.
//    050207 Added 'propfile'.
//
//  Version 1.84.J06.
//    050226 Expanded examples to help text moved from 'GUI' and 'convert'.
//
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//           Removed 'Bug reports to..' from ABOUT.
//
//  Version 1.85.J01.
//    050731 Changed version numbers and copyright.
//
//  Version 1.86.J01.
//    061228 Changed version numbers and copyright.
//    061229 Changed 'verbose' to indicate compact / normal / verbose.
//           Removed 'terse'. Added 'oneline'.
//    070103 Added method 'showAbout' and variable 'gui'.
//
//  Version 1.87.J01.
//    091024 Used generics for 'filenames'.
//    091028 Changed version numbers and copyright years in 'ABOUT'
//           Added warning about obsolete currency rates.
//    091103 Added method 'getPersonalUnits'.
//
//  Version 1.88.J01.
//    101031 Changed version numbers and copyright years.
//           Renamed 'ABOUT' to 'COPYRIGHT' and removed version info.
//           Changed 'showAbout' to show version and invocation info
//           before copyright.
//
//  Version 1.88.J02.
//    110219 Changed version numbers and copyright years.
//    110404 Lines in COPYRIGHT made shorter to fit smaller window.
//
//  Version 1.88.J03.
//    110623 Changed version number.
//           Moved EXAMPLES text to 'convert'.
//
//  Version 1.88.J04.
//    110814 Changed version number.
//
//  Version 1.89.J01.
//    120123 Changed version number of Java Units, original Units,
//           and 'units.dat' file.
//           Changed return type of 'open' in 'FileAcc' to InputStream.
//    120126 Added variable for option '-r'.
//    120228 Removed the error writer 'Env.err'.
//           Replaced use of 'Env.err' by 'Env.out'.
//    120311 Added method 'convert'.
//    120326 Removed 'gui' - never used.
//           Added 'encoding', 'font', and their default values.
//           Added 'guiFont'.
//           Renamed DEFAULTLOCALE to LOCALE.
//           Modified 'getProperties' to obtain ENCODING and GUIFONT.
//           Moved FileAcc and 'files' to UnitsFile.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Vector;
import java.util.Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class Env
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Contains static constants, variables, and methods common
 *  to different components and modes of invocation
 *  Is never instantiated.
 */
 public class Env
{
  //-------------------------------------------------------------------
  //  Constants
  //-------------------------------------------------------------------
  public static final String PROGNAME = "gnu.units";  // Used in error messages
  public static final String VERSION = "1.89.J01";    // Program version
  public static final String ORIGVER = "1.89e";       // Original version
  public static final String UNITSFILE = "units.dat"; // Default units file
  public static final String FILEVER = "Version 1.53 (17 November 2011)";
  public static final String PROPFILE = "units.opt";  // Properties file
  public static final String LOCALE = "en_US";        // Default locale
  public static final int    MAXFILES = 25;           // Max number of units files
  public static final int    MAXINCLUDE = 5;          // Max depth of include files


  public static final String COPYRIGHT = ""
    + "This is an extended Java version of GNU Units " + ORIGVER + ", a program\n"
    + "written in C by Adrian Mariano, copyright (C) 1996, 1997, 1999,\n"
    + "2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2010, 2011 by\n"
    + "Free Software Foundation, Inc.\n"
    + "Java version copyright (C) 2003, 2004, 2005, 2006, 2007, 2008,\n"
    + "2009, 2010, 2011, 2012 by Roman R Redziejowski.\n"
    + "The program is free software; you can redistribute it and/or\n"
    + "modify under the terms of the GNU General Public License\n"
    + "as published by the Free SoftwareFoundation; either version 3\n"
    + "of the License or (at your option) any later version.\n"
    + "The program is distributed in the hope that it will be useful,\n"
    + "but WITHOUT ANY WARRANTY; without even the implied warranty\n"
    + "of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n"
    + "For more details, see the GNU General Public License\n"
    + "(http://www.gnu.org/licenses/).";

  //-------------------------------------------------------------------
  /** Property file if there was one, else null.
   *  Set by 'getProperties'. */
  //-------------------------------------------------------------------
  public static String propfile = null;

  //=====================================================================
  //  Options. Set by UnitsWindow, convert, and applet.
  //  They are first set to default values for each environment; then:
  //  - UnitsWindow may override 'filenames', 'locale', 'encoding',
  //    and 'guifont' by values obtained from property file.
  //  - convert may override 'filenames', 'locale', 'encoding',
  //    and 'guifont' by values obtained from property file,
  //    then override all by values specified as command options.
  //  - applet may override 'location' and 'guifont'
  //    by values from parameters.
  //=====================================================================
  public static Vector<String> filenames;  // Unit definition files
  public static String  locale;            // Locale in effect
  public static String  encoding;          // Encoding name for System io
  public static int     verbose;           // 0=compact, 1=normal, 2=verbose
  public static boolean quiet;             // Suppress prompting and statistics
  public static boolean oneline;           // Only one line of output
  public static boolean strict;            // Strict conversion
  public static boolean unitcheck;         // Unit checking
  public static boolean round;             // Round last element of unit list



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Writer
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  /**
   *  Output writer.
   *  Different subclasses of Writer specify different ways
   *  to write standard output in different environments.
   *  An object of the proper subclass for current environment
   *  is instantiated and assigned to 'out'.
   *  <ul>
   *  <li>GUI defines subclass 'GUI.myOut' and plugs its instance into 'out'.
   *  <li>convert defines subclass 'GUI.myOut' and plugs its instance into 'out'.
   *  </ul>
   */
  abstract public static class Writer
  {
    public abstract void print(final String s);
    public abstract void println(final String s);
  }

  //-------------------------------------------------------------------
  //  Current Writer
  //-------------------------------------------------------------------
  public static Writer out;



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
        Env.out.println(PROGNAME + ": error reading properties from '" + propPath +"'.\n" + e);
        return;
      }

      propfile = propPath; // Property file found and read.

      //---------------------------------------------------------------
      //  If UNITSFILE defined, it is a semicolon-separated list
      //  of file names. Convert it to a vector and store as Env.filenames.
      //---------------------------------------------------------------
      String prop = props.getProperty("UNITSFILE");
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

      //---------------------------------------------------------------
      //  If LOCALE defined, store it as Env.locale.
      //---------------------------------------------------------------
      prop = props.getProperty("LOCALE");
      if (prop!=null) Env.locale = prop.trim();

      //---------------------------------------------------------------
      //  If ENCODING defined, store it as Env.encoding.
      //---------------------------------------------------------------
      prop = props.getProperty("ENCODING");
      if (prop!=null) Env.encoding = prop.trim();
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

  //=====================================================================
  //  convert
  //=====================================================================
  /**
   *  Performs the computation specified by the user.
   *  This is the main work horse of 'units'.
   *
   *  @param  fromExpr expression specifying the value to be converted.
   *  @param  fromValue the value to be converted, completely reduced.
   *  @param  toString string specifying the desired result:
   *          expression, function name, unit list name, or unit list.
   *  @return <code>true</code> if conversion was successful,
   *          <code>false</code> otherwise.
   */
  public static boolean convert
    (final String fromExpr, final Value fromValue, final String toString)
    {
      //---------------------------------------------------------------
      //  If 'toString' is a unit list or name of a unit list,
      //  show conversion to unit list.
      //---------------------------------------------------------------
      String uList = UnitList.isUnitList(toString);

      if (uList!=null)
      {
        UnitList ul = null;
        try
        { ul = new UnitList(uList); }
        catch(EvalError ee)
        {
          Env.out.println("Invalid unit list. " + ee.getMessage());
          return false;
        }

        boolean ok = ul.convert(fromExpr,fromValue);
        return ok;
      }

      //---------------------------------------------------------------
      //  If 'toString' is a function name without argument,
      //  show conversion to that function and return.
      //---------------------------------------------------------------
      DefinedFunction func = DefinedFunction.table.get(toString);
      if (func!=null)
      {
        boolean ok = func.convert(fromExpr,fromValue);
        return ok;
      }

      //---------------------------------------------------------------
      //  Evaluate 'toString' to Value 'toValue'.
      //  A failed evaluation prints error message and returns null.
      //---------------------------------------------------------------
      Value toValue = Value.fromString(toString);
      if (toValue==null)
        return false;

      //---------------------------------------------------------------
      //  Evaluation successful, show conversion.
      //---------------------------------------------------------------
      boolean ok = Value.convert(fromExpr,fromValue,toString,toValue);
      return ok;
    }

}
