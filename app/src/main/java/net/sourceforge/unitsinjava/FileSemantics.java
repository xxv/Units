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
//  Version 1.89.J01.
//   120201 Created for this version.
//
//=========================================================================

package net.sourceforge.unitsinjava;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class FileSemantics
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Contains semantic procedures for FileParser.
 */
 public class FileSemantics extends net.sourceforge.unitsinjava.SemanticsBase
{
  //-------------------------------------------------------------------
  //  Location of line being parsed.
  //-------------------------------------------------------------------
  Location loc;

  //-------------------------------------------------------------------
  //  Include depth.
  //-------------------------------------------------------------------
  int depth;

  //-------------------------------------------------------------------
  //  Indicator: if true we are currently reading data
  //  for the wrong locale so we should skip it.
  //-------------------------------------------------------------------
  boolean wronglocale = false;

  //-------------------------------------------------------------------
  //  Indicator: if true we are currently reading data
  //  for some locale (right or wrong).
  //-------------------------------------------------------------------
  boolean inlocale = false;


  //=====================================================================
  //  command = "locale " argument EOL
  //               0         1      2
  //=====================================================================
  void locale()
    {
      String argument = rhs(1).text().trim();

      if (inlocale)
      {
        Env.out.println(loc.where() + ". Nested locales are not allowed.");
        return;
      }

      if (argument.length() == 0)
      {
        Env.out.println(loc.where() + ". No locale specified.");
        return;
      }

      inlocale = true;
      if (!argument.equals(Env.locale))
      {
        wronglocale = true;
        return;
      }

    }

  //=====================================================================
  //  command = "locale" EOL
  //               0      1
  //=====================================================================
  void badloc()
    {
      Env.out.println(loc.where() + ". No locale specified.");
      return;
    }

  //=====================================================================
  //  command = "endlocale" EOL
  //=====================================================================
  void endlocale()
    {
      if (!inlocale)
      {
        Env.out.println(loc.where() + ". Unmatched !endlocale.");
        return;
      }

      inlocale = false;
      wronglocale = false;
    }

  //=====================================================================
  //  command = "include " argument EOL
  //                0          1     2
  //=====================================================================
  void include()
    {
      if (wronglocale) return;

      String argument = rhs(1).text().trim();

      if (argument.length() == 0)
      {
        Env.out.println(loc.where() + ". No file name specified.");
        return;
      }

      if (depth>=Env.MAXINCLUDE)
      {
        Env.out.println(loc.where() + ". Max include depth of "
                        + Env.MAXINCLUDE + " exceeded.");
        return;
      }

      UnitsFile infile = new UnitsFile(argument);
      boolean ok = infile.readunits(depth+1);
      if (ok) Env.filenames.add(infile.name);
    }

  //=====================================================================
  //  command = "include" EOL
  //               0      1
  //=====================================================================
  void badincl()
    {
      Env.out.println(loc.where() + ". No file name specified.");
      return;
    }

  //=====================================================================
  //  command = "unitlist " space name space argument EOL
  //                0         1     2    3      4      5
  //=====================================================================
  void unitlist()
    {
      if (wronglocale) return;

      String name = rhs(2).text();
      String argument = rhs(4).text().trim();

      if (argument.length() == 0)
      {
        Env.out.println
          (loc.where() + ". No unit list specified for '" + name + "'.");
        return;
      }

      Alias.define(name,argument,loc);
    }

  //=====================================================================
  //  command = "unitlist" space EOL
  //                0        1   2
  //=====================================================================
  void badlist()
    {
      Env.out.println
        (loc.where() + ". No name specified with '!unitlist'.");
    }

  //=====================================================================
  //  command = skip EOL
  //          0    1
  //=====================================================================
  void badcomm()
    {
      Env.out.println(loc.where() + ". Unrecognized command '!"
                      + rhsText(0,1) + "'.");
    }

  //=====================================================================
  //  definition = name "(" param ")" space "[" dim ";" dim "]" def (";" def)? EOL
  //                 0   1    2    3    4    5   6   7   8   9   10   11  12   11,13
  //=====================================================================
  void funcdef1()
    {
      if (wronglocale) return;

      String name = rhs(0).text();
      String param = rhs(2).text();
      String fwddim = rhs(6).text().trim();
      String invdim = rhs(8).text().trim();
      String fwddef = rhs(10).text().trim();
      String invdef = null;
      if (rhsSize()>12) invdef = rhs(12).text().trim();

      if (fwddef.length() == 0)
      {
        Env.out.println
          (loc.where() + ". Function '" + name +
           "' is ignored. Definition missing.");
        return;
      }

      if (fwddim.length() == 0) fwddim = null;
      if (invdim.length() == 0) invdim = null;

      ComputedFunction.define
        (name,param,fwddim,invdim,fwddef,invdef,loc);
    }

  //=====================================================================
  //  definition = name "(" param ")" def (";" def)? EOL
  //                 0   1    2    3   4    5   6    5,7
  //=====================================================================
  void funcdef2()
    {
      if (wronglocale) return;

      String name = rhs(0).text();
      String param = rhs(2).text();
      String fwddef = rhs(4).text().trim();
      String invdef = null;
      if (rhsSize()>6) invdef = rhs(6).text().trim();

      if (fwddef.length() == 0)
      {
        Env.out.println
          (loc.where() + ". Function '" + name +
           "' is ignored. Definition missing.");
        return;
      }

      ComputedFunction.define
        (name,param,null,null,fwddef,invdef,loc);
    }

  //=====================================================================
  //  definition = name "(" param ")" space "[" skip EOL
  //                 0   1    2    3    4    5   6    7
  //=====================================================================
  void baddim()
    {
      if (wronglocale) return;

      String name = rhs(0).text();

      Env.out.println
        (loc.where() + ". Function '" + name +
         "' is ignored. Argument dimensions not found after '['.");
    }

  //=====================================================================
  //  definition = name "(" skip EOL
  //                 0   1    2   3
  //=====================================================================
  void badfunc()
    {
      if (wronglocale) return;

      String name = rhs(0).text();

      Env.out.println
        (loc.where() + ". Function '" + name +
         "' is ignored. Parameter not found after '('.");
    }

  //=====================================================================
  //  definition = name "[" resUnit "]" space pair+ EOL
  //                 0   1     2     3    4   5,..  6,..
  //=====================================================================
  void tabdef()
    {
      if (wronglocale) return;

      String name = rhs(0).text();
      String resUnit = rhs(2).text().trim();
      int npairs = rhsSize()-6;

      if (npairs==1)
      {
        Env.out.println
        (loc.where() + ". Function '" + name +
         "' is ignored. Only one point is defined.");
        return;
      }

      double x[] = new double[npairs];
      double y[] = new double[npairs];

      for (int i=0;i<npairs;i++)
      {
        Pair pair = (Pair)rhs(i+5).get();
        x[i] = pair.x;
        y[i] = pair.y;
      }

      TabularFunction.define(name,resUnit,x,y,loc);
    }

  //=====================================================================
  //  definition =  name "[" skip EOL
  //                  0   1    2   3
  //=====================================================================
  void badtab()
    {
      if (wronglocale) return;

      String name = rhs(0).text();

      Env.out.println
        (loc.where() + ". Function '" + name +
         "' is ignored. Result unit not found after '['.");
    }

  //=====================================================================
  //  definition = name [ \t] def EOL
  //                 0    1    2   3
  //=====================================================================
  void unitdef()
    {
      if (wronglocale) return;

      String name = rhs(0).text();
      String def = rhs(2).text().trim();

      if (def.length() == 0)
      {
        Env.out.println
          (loc.where() + ". Unit '" + name +
           "' is ignored. Definition missing.");
        return;
      }

      //---------------------------------------------------------------
      //  If unitname ends with '-', we have a prefix definition.
      //---------------------------------------------------------------
      if (name.endsWith("-"))
        Prefix.define(name,def,loc);

      //---------------------------------------------------------------
      // Otherwise we have a unit definition.
      //---------------------------------------------------------------
      else
        Unit.define(name,def,loc);
    }


  //=====================================================================
  //  definition = name EOL
  //                 0   1
  //=====================================================================
  void badunit()
    {
      if (wronglocale) return;

      String name = rhs(0).text();

      Env.out.println
        (loc.where() + ". Unit '" + name +
         "' is ignored. Definition missing.");
    }


  //=====================================================================
  //  pair = number number ("," space)?
  //           0      1      2    3
  //=====================================================================
  void pair()
    { lhs().put(new Pair((Double)rhs(0).get(),(Double)rhs(1).get())); }


  //=====================================================================
  //  number = sign? mantissa exponent? space
  //=====================================================================
  void number()
    { lhs().put(Double.valueOf(lhs().text())); }



  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
  //
  //  Pair
  //
  //HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

   public class Pair
  {
    double x;
    double y;

    Pair(final Double x, final Double y)
      {
        this.x = x;
        this.y = y;
      }
  }
}
