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
//    091024 Version 1.87.J01.
//           Used generics for 'factors'.
//           Used modified 'insertAlph'.
//    091025 Replaced 'Parser.Exception' by 'EvalError'.
//    091031 Moved definition of Ignore to Factor.
//    091101 'insertAlph' replaced by simple loop.
//
//=========================================================================

package net.sourceforge.unitsinjava;

import java.util.Vector;



//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Product
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A product of units and/or prefixes.
 *  <br>
 *  It may be empty, representing dimensionless number 1.
 */

 public class Product
{
  //-------------------------------------------------------------------
  //  The factors in a Product are represented as element of this Vector.
  //  The factors are Factor objects (units or prefixes).
  //  They are sorted in increasing alphabetic order of their names.
  //  Duplicates are allowed (mean a power>1 of the unit).
  //-------------------------------------------------------------------
  private Vector<Factor> factors = new Vector<Factor>();

  //=====================================================================
  //  Default constructor.
  //=====================================================================
  Product()
    { factors = new Vector<Factor>(); }


  //=====================================================================
  //  Copy constructor.
  //=====================================================================
  Product(final Product p)
    {
      for (Factor e: p.factors)
        factors.add(e);
    }


  //=====================================================================
  //  Add Factor 'f' to the Product.
  //  Return the result.
  //  (Originally 'addsubunit').
  //=====================================================================
  Product add (final Factor f)
    {
      for (int i=0;i<factors.size();i++)
        if (f.compareTo(factors.elementAt(i))<=0)
        {
          factors.add(i,f);
          return this;
        }
      factors.add(f);
      return this;
    }


  //=====================================================================
  //  Add all factors of Product "p" to "this".
  //  Return the result.
  //  (Originally 'addsubunitlist').
  //=====================================================================
  Product add(final Product p)
    {
      for (int i=0;i<p.size();i++)
        add(p.factor(i));
      return this;
    }


  //=====================================================================
  //  Return number of factors.
  //=====================================================================
  public int size()
    { return factors.size(); }


  //=====================================================================
  //  Return i-th factor.
  //=====================================================================
  public Factor factor(int i)
    { return factors.elementAt(i); }


  //=====================================================================
  //  Remove i-th factor.
  //=====================================================================
  public void delete(int i)
    { factors.removeElementAt(i); }

  public Vector<Factor> getFactors(){
    return factors;
  }
  //=====================================================================
  //  Return true if this Product and Product "p" have the same factors,
  //  other than those marked dimensionless.
  //  (Originally 'compareproducts'.)
  //=====================================================================
  boolean isCompatibleWith(final Product p, Factor.Ignore ignore)
    {
      int i = 0;
      int j = 0;

      while(true)
      {
        while(i<size() && factor(i).ignoredIf(ignore)) i++;
        while(j<p.size() && p.factor(j).ignoredIf(ignore)) j++;
        if (i==size() || j==p.size()) break;
        if (factor(i)!=p.factor(j)) return false;
        i++;
        j++;
      }
      if (i==size() && j==p.size()) return true;
      return false;
    }


  //=====================================================================
  //  Return printable representation of the Product.
  //=====================================================================
  public String asString()
    {
      StringBuffer sb = new StringBuffer();
      int counter = 1;

      for (int i=0;i<size();i++)
      {
        Factor f = factor(i);

        // If s is the same as preceding, increment counter.
        if (i>0 && f==factor(i-1))
          counter++;

        // If s is first or distinct from preceding:
        else
        {
          if (counter>1)
            sb.append("^" + counter);
          sb.append(" " + f.name);
          counter = 1;
        }
      }

      if (counter>1)
        sb.append("^" + counter);

      return sb.toString();
    }


  //=====================================================================
  //  Return n-th root of the Product, or null if not n-th root.
  //  (Originally 'subunitroot').
  //=====================================================================
  Product root(int n)
    {
      Product p = new Product();
      for (int i=0;i<size();i++)
      {
        Factor f = factor(i);
        int j = 1;
        i++;
        while (i<size() && f==factor(i))
        {
          i++;
          j++;
        }
        if (j%n!=0) return null; // Not n-th root.

        for (int k=0;k<(j/n);k++)
          p.add(f);
      }

      return p;
    }
} // end of Product
