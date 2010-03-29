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
//           Total rewrite using GUI for interactive invocation
//           and Env for common constants and switches.
//           Properties used to specify locale and unit files.
//    050226 Version 1.84.J06.
//           Help text examples expanded and moved to 'Env'.
//           Added info about help to 'USAGE'.
//    050315 Version 1.84.J07.
//           Changed package name to "units".
//    061229 Version 1.86.J01.
//           New setting of options.
//           Included new options in USAGE.
//    061231 Used Tables.stat to print table statistics.
//    070102 Suppress printing "\tDefinition :" for compact output.
//    091024 Version 1.87.J01.
//           Adjusted to new interface of CommandArgs.
//    091101 Implemented 'search text'.
//    091103 Corrected description of option -f "" (was -f '').
//           Implemented personal units file.
//    091105 No table statistics on non-interactive use.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class convert
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Holds the main method and its subroutines.
 */

class convert
{
  //=====================================================================
  //  Constants.
  //=====================================================================
  static final String USAGE =
    "Usage:\n\n"
     + "  java -jar <jarfile> [options] [from-unit [to-unit]]\n\n"
     + "Call without 'options', 'from-unit', and 'to-unit' opens graphical interface.\n"
     + "Options are:\n\n"
     + "  -c  suppress printing of tab, '*', and '/' character\n"
     + "  -f  specify a units data file (-f \"\" loads default file)\n"
     + "  -h  print this help and exit\n"
     + "  -i  use interactively from command prompt\n"
     + "  -l  specify locale\n"
     + "  -q  suppress prompting\n"
     + "  -s  suppress reciprocal unit conversion (e.g. Hz<->s)\n"
     + "  -t  terse output (-c -q -s -1)\n"
     + "  -v  print slightly more verbose output\n"
     + "  -1  suppress the second line of output\n"
     + "  -C  check that all units reduce to primitive units\n"
     + "  -V  describe this version of Units and exit\n\n"
     + "After starting the program, type 'help' at a prompt\n"
     + "or click on 'Help' button for more help.";

  static final String HELP =
    " Units converts between different measuring systems.\n"
     + " Type the measure you want to convert at the 'You have:' prompt.\n"
     + " Type the desired units at the 'You want:' prompt.\n\n"
     + " Press return at the 'You want:' prompt to see the measure you entered above\n"
     + " reduced to primitive units.\n"
     + " Type '?' at 'You want:' prompt to get a list of conformable units.\n\n"
     + " At either prompt you can type:\n"
     + "  'help' to see this message, or\n"
     + "  'help unit' to explore units database around the definition of 'unit', or\n"
     + "  'search text' to see the units whose name contains 'text'.\n\n"
     + " Type 'quit' at either prompt to quit.\n\n";


  //=====================================================================
  //  Writers.
  //=====================================================================
  private static class myOut extends Env.Writer
  {
    @Override
	public
	void print(final String s)   {System.out.print(s);};
    @Override
	public
	void println(final String s) {System.out.println(s);};
  }

  private static class myErr extends Env.Writer
  {
    @Override
	public
	void print(final String s)   {System.err.print(s);};
    @Override
	public
	void println(final String s) {System.err.println(s);};
  }


  //=====================================================================
  //  main
  //=====================================================================
  /**
   *  Main entry to 'convert'
   *
   *  @param argv command arguments
   */
  public static void main(final String[] argv)
    {
      //---------------------------------------------------------------
      //  If invoked without options or arguments: open GUI.
      //---------------------------------------------------------------
      if (argv.length==0)
      {
        return;
      }

      //---------------------------------------------------------------
      //  We arrive here only for use from command prompt.
      //  Set up default values for options.
      //  (May be changed in the next steps by values from properties
      //  file and from command line.)
      //---------------------------------------------------------------
      Env.filenames = new Vector<String>(); // Unit definition files
      Env.getPersonalUnits();
      Env.filenames.add(Env.UNITSFILE);

      Env.locale = Env.DEFAULTLOCALE;   // Locale
      Env.verbose = 1;                  // Neither verbose nor compact
      Env.quiet = false;                // Don't suppress prompting / statistics
      Env.oneline = false;              // Print both lines of result
      Env.strict = false;               // Allow reciprocal conversion
      Env.unitcheck = false;            // No unit checking

      //---------------------------------------------------------------
      //  Set up file access and writers.
      //---------------------------------------------------------------
      Env.files = new File.StandAcc();  // File access
      Env.out = new myOut();            // Writers
      Env.err = new myErr();

      //---------------------------------------------------------------
      //  Get option values from properties file (if any).
      //---------------------------------------------------------------
      Env.getProperties();

      //---------------------------------------------------------------
      //  Parse command argument vector 'argv'.
      //  Write usage message and return if error detected.
      //---------------------------------------------------------------
      final CommandArgs cmd = new CommandArgs(argv,"chiqstv1CV","fl",0,2);
      if (cmd.nErrors()>0)
      {
        Env.out.println(USAGE);
        System.exit(1);
      }

      //---------------------------------------------------------------
      //  Object 'cmd' represents now parsed 'argv' vector.
      //  Return if -h and/or -V was specified.
      //  Otherwise save information specified by options.
      //---------------------------------------------------------------
      final boolean done = processopts(cmd);
      if (done) {
		System.exit(0);
	}

      //---------------------------------------------------------------
      //  Build tables.
      //---------------------------------------------------------------
      final boolean ok = Tables.build();
      if (!ok) {
		System.exit(1);
	}

      //---------------------------------------------------------------
      //  If requested, do unit check and return.
      //---------------------------------------------------------------
      if (Env.unitcheck)
      {
        Tables.check();
        System.exit(0);
      }

      //---------------------------------------------------------------
      //  Proceed to do conversions.
      //---------------------------------------------------------------
      if (cmd.nArgs()>0) {
		noninteractive(cmd);
	} else {
		interactive();
	}

      System.exit(0);
    }


  //=====================================================================
  //  noninteractive
  //=====================================================================
  /**
   *  Does non-interactive conversion.
   */
  private static void noninteractive(CommandArgs cmd)
    {
      String havestr;
      String wantstr;
      Value have;
      Value want;
      Function func;

      Env.quiet = true;
      havestr = cmd.arg(0);    // Get 'from-unit' to 'havestr'

      if (cmd.nArgs()>1) {
		wantstr = cmd.arg(1);  // ..get it to 'wantstr'.
	} else {
		wantstr = null;         // Otherwise set 'wantstr' to null.
	}

      //---------------------------------------------------------------
      //  If 'from-unit' is a function name without argument,
      //  show definition of that function and return.
      //---------------------------------------------------------------
      func = DefinedFunction.table.get(havestr);
      if (func!=null)
      {
        Env.out.println(func.showdef());
        System.exit(0);
      }

      //---------------------------------------------------------------
      //  Convert 'from-unit' to Value 'have'.
      //  A failed conversion prints error message and returns null.
      //---------------------------------------------------------------
      have = Value.fromString(havestr);
      if (have==null) {
		System.exit(1);
	}

      //---------------------------------------------------------------
      //  We know now that 'from-unit' correctly specifies a Value.
      //  If 'to-unit' was not specified, show definition of 'from-value'.
      //---------------------------------------------------------------
      if (wantstr==null)
      {
        if (Env.verbose>0) {
			Env.out.print("\tDefinition: ");
		}
        Factor.showdef(havestr);
        have.show();
        System.exit(0);
      }

      //---------------------------------------------------------------
      //  We arrive here if 'to-unit' was specified.
      //  If 'to-unit' is a function name without argument,
      //  show conversion to that function and return.
      //---------------------------------------------------------------
      func = DefinedFunction.table.get(wantstr);
      if (func!=null)
      {
        Value.convert(havestr,have,func);
        System.exit(0);
      }

      //---------------------------------------------------------------
      //  Convert 'to-unit' to Value 'want'.
      //  A failed conversion prints error message and returns null.
      //---------------------------------------------------------------
      want = Value.fromString(wantstr);
      if (want==null) {
		System.exit(1);
	}

      //---------------------------------------------------------------
      //  Conversion successful, show answer.
      //---------------------------------------------------------------
      Value.convert(havestr,have,wantstr,want);
      System.exit(0);
    }


  //=====================================================================
  //  interactive
  //=====================================================================
  /**
   *  Does interactive conversions.
   */
  private static void interactive()
    {
      String havestr;
      String wantstr;
      Value have;
      Value want;
      Function func;

      //-------------------------------------------------------------
      //  Create reader for user's input
      //-------------------------------------------------------------
      final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

      //-------------------------------------------------------------
      //  Print table statistics if not suppressed.
      //-------------------------------------------------------------
      if (!Env.quiet) {
		Env.out.println(Tables.stat());
	}

      //-------------------------------------------------------------
      //  Keep doing conversions.
      //  This 'while' statement is terminated by 'return'.
      //-------------------------------------------------------------
      loop:
      while(true)
      {
        have = null;
        want = null;
        func = null;

        //-----------------------------------------------------------
        //  Keep prompting the user with "You have:"
        //  until the user enters a valid unit expression or
        //  a function name without argument.
        //  If the user enters 'help' or 'help unitname', provide help
        //  and prompt again.
        //  Return if the user enters empty line or 'quit'.
        //-----------------------------------------------------------
        do
        {
          havestr = getuser("You have: ",in);
          if (havestr==null || havestr.equals("quit")) {
			break loop;
		}
          if (!ishelpquery(havestr,null))
          {
            func = DefinedFunction.table.get(havestr);
            if (func==null) {
				have = Value.fromString(havestr);
			}
          }
        } while (have==null && func==null);

        //-----------------------------------------------------------
        //  If the user entered function name, print definition
        //  of that function and prompt again with '"You have: "'.
        //-----------------------------------------------------------
        if (func!=null)
        {
          Env.out.println(func.showdef());
          continue;
        }

        //-----------------------------------------------------------
        //  User entered a valid unit expression.
        //  The Value represented by the expression is in 'have'.
        //  Keep prompting the user with "You want:"
        //  until the user enters empty line, a valid unit expression,
        //  or a function name without argument.
        //  If the user enters 'help', 'help unitname', or '?',
        //  provide help and prompt again.
        //  Return if the user enters 'quit'.
        //-----------------------------------------------------------
        do
        {
          wantstr = getuser("You want: ",in);
          if (wantstr==null) {
			break;
		}
          if (wantstr.equals("quit")) {
			break loop;
		}
          if (!ishelpquery(wantstr,have))
          {
            func = DefinedFunction.table.get(wantstr);
            if (func==null) {
				want = Value.fromString(wantstr);
			}
          }
        } while (want==null && func==null);

        //-----------------------------------------------------------
        //  If user entered empty line, show definition of 'have'.
        //-----------------------------------------------------------
        if (wantstr==null)
        {
          if (Env.verbose>0) {
			Env.out.print("\tDefinition: ");
		}
          Factor.showdef(havestr);
          have.show();
        }

        //-----------------------------------------------------------
        //  If user entered function name, show conversion of 'have'
        //  to that function.
        //-----------------------------------------------------------
        else if (func!=null) {
			Value.convert(havestr,have,func);
		} else {
			Value.convert(havestr,have,wantstr,want);
		}

      } // end of prompting loop
    }


  //=====================================================================
  //  processopts
  //=====================================================================
  /**
   *  Obtains options specified by the command.
   *  Prints help or version if -h and/or -V option is present.
   *
   *  @param  cmd CommandArgs object containing parsed command arguments.
   *  @return true if help or version was printed,
   *          false otherwise.
   */
  private static boolean processopts(CommandArgs cmd)
    {
      if (cmd.opt('h'))
      {
        Env.out.println(USAGE);
        return true;
      }

      if (cmd.opt('V'))
      {
        Env.out.println(Env.ABOUT);
        return true;
      }

      if (cmd.opt('f')) {
		Env.filenames = cmd.optArgs('f');
	}
      if (cmd.opt('l')) {
		Env.locale = cmd.optArg('l');
	}

      if (cmd.opt('v')) {
		Env.verbose = 2;
	}
      if (cmd.opt('c')) {
		Env.verbose = 0;
	}
      if (cmd.opt('q')) {
		Env.quiet = true;
	}
      if (cmd.opt('1')) {
		Env.oneline = true;
	}
      if (cmd.opt('s')) {
		Env.strict = true;
	}
      if (cmd.opt('C')) {
		Env.unitcheck = true;
	}
      if (cmd.opt('t'))
      {
        Env.verbose = 0;
        Env.quiet=true;
        Env.strict=true;
        Env.oneline = true;
      }

      return false;
    }


  //=====================================================================
  //  ishelpquery
  //=====================================================================
  /**
   *  Provides help in interactive mode.
   *  <br>
   *  If 'have' is not null and the string 's' contains UNITMATCH ("?")
   *  character, shows all units conformable to Value 'have' and returns true.
   *  If 's' is HELPCOMMAND ("help"), prints general help and returns true.
   *  If 's' is HELPCOMMAND followed by a unit name, shows part of
   *  definition file for that unit and returns true.
   *  Otherwise returns false.
   *
   *  @param  s string to be checked for being request for help.
   *  @param  have Value to show conformable units.
   *  @return true if help was requested (and provided).
   */
  static private boolean ishelpquery
    ( final String s, final Value have)
    {
      final String HELPCOMMAND = "help";           // Command to request help at prompt
      final String SEARCHCOMMAND = "search";       // Command to request search
      final String UNITMATCH = "?";                // Command to request conformable units
      final String WHITE = " \t";                  // Whitespace characters

      final int hl = HELPCOMMAND.length();
      final int sl = SEARCHCOMMAND.length();

      String str = s.trim();

      //---------------------------------------------------------------
      //  Request for conformable units?
      //---------------------------------------------------------------
      if (have!=null && str.indexOf(UNITMATCH)>=0)
      {
        Tables.showConformable(have,s);
        return true;
      }

      //---------------------------------------------------------------
      //  Request for help?
      //---------------------------------------------------------------
      if (str.equals(HELPCOMMAND))
      {
        return true;
      }

      //---------------------------------------------------------------
      //  Request for source? ("help unit")
      //---------------------------------------------------------------
      if (str.length()>hl &&
          HELPCOMMAND.equals(str.substring(0,hl)) &&
          WHITE.indexOf(str.charAt(hl))>=0)
      {
        str = str.substring(hl,str.length()).trim();
        Tables.showSource(str);
        return true;
      }

      //---------------------------------------------------------------
      //  Request for search? ("search text")
      //---------------------------------------------------------------
      if (str.length()>sl &&
          SEARCHCOMMAND.equals(str.substring(0,sl)) &&
          WHITE.indexOf(str.charAt(sl))>=0)
      {
        str = str.substring(sl,str.length()).trim();
        Tables.showMatching(str);
        return true;
      }

      return false;
    }


  //=====================================================================
  //  getuser
  //=====================================================================
  /**
   *  Prompts the user and reads reply in the interactive mode.
   *
   *  @param  prompt the prompt string.
   *  @param  in reader to obtain reply from.
   *  @return user's reply with leading and trailing blanks removed,
   *          or null if the reply was empty line.
   */
  private static String getuser(final String prompt,BufferedReader in)
    {
      if (!Env.quiet) {
		Env.out.print(prompt);
	}

      String reply;
      try
      { reply = in.readLine().trim(); }
      catch (final IOException e)
      {
        Env.err.println(e.toString());
        return null;
      }
      return reply.length()==0? null : reply;
    }
}


