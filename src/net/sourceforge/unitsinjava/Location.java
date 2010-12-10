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
//    050315 Version 1.84.J07. Changed package name to "units".
//
//=========================================================================

package net.sourceforge.unitsinjava;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Location
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Identifies location of a piece of text in a file.
 */
 public class Location
{
  final File file;     // File
  final int lineNum;   // Line number
  final int beginChar; // Starting character index
  final int endChar;   // Ending character index


//=======================================================================
//  Constructor
//=======================================================================
/**
 *  Constructs dummy Location for built-in entity.
 */
Location()
  {
    file = null;
    lineNum = -1;
    beginChar = -1;
    endChar = -1;
  }


//=======================================================================
//  Constructor
//=======================================================================
/**
 *  Constructs Location object.
 *
 *  @param  fil file name.
 *  @param  line line number.
 *  @param  begin starting character index.
 *  @param  end ending character index.
 */
Location(final File fil, int line, int begin, int end)
  {
    file = fil;
    lineNum = line;
    beginChar = begin;
    endChar = end;
  }
}
