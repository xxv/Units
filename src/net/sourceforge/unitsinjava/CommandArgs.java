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
//    050203 Version 1.84.J05. User interface redesigned.
//           Instead of cycling through options, you ask directly
//           if a given option was specified and how many times.
//           You can directly get a list of all values specified
//           with that option.
//           Attribute 'public' removed from all methods.
//    050315 Version 1.84.J07. Changed package name to "units".
//    091024 Version 1.87.J01.
//           Used generics and modernized access to options.
//    091103 Corrected bug: exception on empty string as option argument.
//    091104 'String.isEmpty' not accepted by JDK1.5. Changed to length==0.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Vector;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Class CommandArgs
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Object-oriented counterpart of C procedure 'getopt':
 *  an object of class CommandArgs represents parsed argument list
 *  of a command (the 'argv' parameter to 'main').
 *  <p>
 *  The list is supposed to follow POSIX conventions,
 *  (IEEE Standard 1003.1, 2004 Edition, Chapter 12), namely:
 *  <ul>
 *  <li>The list consist of <i>options</i> and/or <i>arguments</i>.
 *  <li>The options precede the arguments.
 *  <li>An option is a hyphen followed by a single alphanumeric character,
 *        like this: <code>-o</code>.
 *  <li>An option may require a <i>value</i>, which may appear immediately after the
 *      option character, for example: <code>-ovalue</code>,
 *      or as the following item in the list, for example: <code>-o value</code>.
 *  <li>Options that do not require value can be grouped after a hyphen, so,
 *      for example, <code>-rst</code> is equivalent to <code>-r -s -t</code>.
 *  <li>Options can appear in any order; thus <code>-rst</code>
 *      is equivalent to <code>-trs</code>.
 *  <li>Options can appear multiple times.
 *  <li>The <code>--</code> item is neither an option nor an argument.
 *      It terminates options.
 *      Anything that follows is an argument, even if it begins with a hyphen.
 *  <li>A hyphen alone is an argument. It terminates options.
 *  </ul>
 *  A CommandArgs object is constructed by parsing the argument list
 *  according to instructions supplied to the constructor.
 *  The options and arguments can be then obtained by invoking
 *  methods on that object.
 */


public class CommandArgs
{
  //-------------------------------------------------------------------
  //  Option letters in order of appearance.
  //  Note that options with argument may have multiple occurrences.
  //-------------------------------------------------------------------
  private String letters;

  //-------------------------------------------------------------------
  //  Arguments specified with letters.
  //  Null for argument-less options.
  //-------------------------------------------------------------------
  private Vector<String> optArgs = new Vector<String>();

  //-------------------------------------------------------------------
  //  Positional arguments.
  //-------------------------------------------------------------------
  private Vector<String> args = new Vector<String>();

  //-------------------------------------------------------------------
  //  Error count.
  //-------------------------------------------------------------------
  private int errors = 0;

  //-------------------------------------------------------------------
  /** Construct CommandArgs object from an argument list 'argv'.
  *   <br>
  *   @param  argv Argument list, as passed to the program.
  *   @param  options String consisting of option letters for options without argument.
  *   @param  optionsWithArg String consisting of option letters for options with argument.
  *   @param  minargs Minimum number of arguments.
  *   @param  maxargs Maximum number of arguments.
  */
  //-------------------------------------------------------------------
  public CommandArgs
    ( final String[] argv,
      final String options,
      final String optionsWithArg,
      int minargs,
      int maxargs)
    {
      int i = 0;
      StringBuffer opts = new StringBuffer();

      //---------------------------------------------------------------
      //  Examine elements of argv as long as they specify options.
      //---------------------------------------------------------------
      while(i<argv.length)
      {
        String elem = argv[i];

        //-------------------------------------------------------------
        //  Any element that does not start with '-' terminates options
        //-------------------------------------------------------------
        if (elem.length()==0 || elem.charAt(0)!='-')
          break;

        //-------------------------------------------------------------
        //  A single '-' is a positional argument and terminates options.
        //-------------------------------------------------------------
        if (elem.equals("-"))
        {
          args.addElement("-");
          i++;
          break;
        }

        //-------------------------------------------------------------
        //  A  '--' is not an argument and terminates options.
        //-------------------------------------------------------------
        if (elem.equals("--"))
        {
          i++;
          break;
        }

        //-------------------------------------------------------------
        //  An option found - get option letter.
        //-------------------------------------------------------------
        String c = elem.substring(1,2);

        if (optionsWithArg.indexOf(c)>=0)
        {
          //-----------------------------------------------------------
          //  Option with argument
          //-----------------------------------------------------------
          opts.append(c);
          if (elem.length()>2)
          {
            // option's argument in the same element
            optArgs.addElement(elem.substring(2,elem.length()));
            i++;
          }

          else
          {
            // option's argument in next element
            i++;
            if (i<argv.length && (argv[i].length()==0 || argv[i].charAt(0)!='-'))
            {
              optArgs.addElement(argv[i]);
              i++;
            }
            else
            {
              System.err.println("Missing argument of option -" + c + ".");
              optArgs.addElement(null);
              errors++;
            }
          }
        }

        else
        {
          //-----------------------------------------------------------
          //  Option without argument or invalid.
          //  The element may specify more options.
          //-----------------------------------------------------------
          for (int n=1;n<elem.length();n++)
          {
            c = elem.substring(n,n+1);
            if (options.indexOf(c)>=0)
            {
              opts.append(c);
              optArgs.addElement(null);
            }
            else
            {
              System.err.println("Unrecognized option -" + c + ".");
              errors++;
              break;
            }
          }
          i++;
        }
      }

      letters = opts.toString();

      //---------------------------------------------------------------
      //  The remaining elements of argv are positional arguments.
      //---------------------------------------------------------------
      while(i<argv.length)
      {
        args.addElement(argv[i]);
        i++;
      }

      if (nArgs()<minargs)
      {
        System.err.println("Missing argument(s).");
        errors++;
      }

      if (nArgs()>maxargs)
      {
        System.err.println("Too many arguments.");
        errors++;
      }
    }

  //-------------------------------------------------------------------
  //  Access to options
  //-------------------------------------------------------------------
  /**
  *  Checks if a given option was specified.
  *
  *  @param  c Option letter.
  *  @return true if the option is specified, false otherwise..
  */
  public boolean opt(char c)
    { return letters.indexOf(c)>=0; }

  /**
  *  Gets argument of a given option.
  *  Returns null if the option is not specified or does not have argument.
  *  If option was specified several times, returns the first occurrence.
  *
  *  @param  c Option letter.
  *  @return value of the i-th option.
  */
  public String optArg(char c)
    {
      int i = letters.indexOf(c);
      return i<0? null : optArgs.elementAt(i);
    }

  /**
  *  Gets arguments of a given option.
  *  Returns a vector of arguments for an option specified repeatedly-
  *  Returns empty vector if the option is not specified or does not have argument.
  *
  *  @param  c Option letter.
  *  @return value of the i-th option.
  */
  public Vector<String> optArgs(char c)
    {
      Vector<String> result = new Vector<String>();
      for (int i=0;i<letters.length();i++)
        if (letters.charAt(i)==c)
          result.add(optArgs.elementAt(i));
      return result;
    }

  //-------------------------------------------------------------------
  //  Access to positional arguments
  //-------------------------------------------------------------------
  /**
  *  Gets the number of arguments in the argument list.
  *
  *  @return Number of arguments.
  */
  public int nArgs()
    { return args.size(); }

  /**
  *  Gets the i-th argument.
  *
  *  @param  i Argument number (<code>0&le;i&lt;nOpts()</code>).
  *  @return the i-th argument.
  */
  public String arg(int i)
    { return args.elementAt(i); }

  /**
  *  Gets the argument vector.
  *
  *  @return Vector<String> of arguments.
  */
  public Vector<String> args()
    { return args; }

  //-------------------------------------------------------------------
  //  Error count
  //-------------------------------------------------------------------
  /**
  *  Gets number of errors detected when parsing the argument list.
  *
  *  @return Number of errors.
  */
  public int nErrors()
    { return errors; }
}