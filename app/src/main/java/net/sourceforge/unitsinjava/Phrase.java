//=========================================================================
//
//  Part of PEG parser generator Mouse.
//
//  Copyright (C) 2009 by Roman R. Redziejowski (www.romanredz.se).
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
//-------------------------------------------------------------------------
//
//  Change log
//    090701 License changed by the author to Apache v.2.
//    090717 Removed unused import of java.util.Vector.
//
//=========================================================================

package net.sourceforge.unitsinjava;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  Phrase seen from Semantics
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

public interface Phrase
{
  //-------------------------------------------------------------------
  //  Set value
  //-------------------------------------------------------------------
  void put(Object o);

  //-------------------------------------------------------------------
  //  Get value
  //-------------------------------------------------------------------
  Object get();

  //-------------------------------------------------------------------
  //  Get text
  //-------------------------------------------------------------------
  String text();

  //-------------------------------------------------------------------
  //  Get i-th character of text
  //-------------------------------------------------------------------
  char charAt(int i);

  //-------------------------------------------------------------------
  //  Is text empty?
  //-------------------------------------------------------------------
  boolean isEmpty();

  //-------------------------------------------------------------------
  //  Is this s?
  //-------------------------------------------------------------------
  boolean isA(String s);

  //-------------------------------------------------------------------
  //  Get error message
  //-------------------------------------------------------------------
  String errMsg();

  //-------------------------------------------------------------------
  //  Clear error message
  //-------------------------------------------------------------------
  void errClear();

}
