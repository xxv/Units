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
//  2009, 2012 by Roman R Redziejowski (www.romanredz.se).
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
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.89.J01.
//    120121 Renamed 'File' to 'UnitsFile'.
//    120129 Added method 'where'.
//
//=========================================================================

package net.sourceforge.unitsinjava;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Location
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  Identifies location of a piece of text in a units file.
 *  Note that line numbers start with 1.
 */
 public class Location
{
  final UnitsFile file; // UnitsFile object for the file.
  final int lineNum;    // Number of the first line containing the text.
  final int beginChar;  // Index of first character in String-mapped file.
  final int endChar;    // Index of last character in String-mapped file.


//=======================================================================
//  Constructor
//=======================================================================
/**
 *  Constructs dummy Location object.
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
 *  @param  file  UnitsFile object for the file.
 *  @param  line  Number of the first line containing the text.
 *  @param  begin Index of first character in String-mapped file.
 *  @param  end   Index of last character in String-mapped file.
 */
Location(final UnitsFile file, int line, int begin, int end)
  {
    this.file = file;
    lineNum = line;
    beginChar = begin;
    endChar = end;
  }


//=======================================================================
//  where
//=======================================================================
/**
 *  @return String that describes the location.
 */
 String where()
   { return "'" + file.name + "', line " + lineNum ; }
}
