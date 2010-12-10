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
//    090810 Package name changed.
//
//=========================================================================

package net.sourceforge.unitsinjava;


public interface Source
{
  boolean created();

  int end();

  char at(int p);

  String at(int p, int q);

  String where(int p);
}
