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
//  Version 1.84.J07.
//    050315 Changed package name to 'units'.
//
//  Version 1.87.J01.
//    091024 Used generics for 'factors'.
//           Used modified 'insertAlph'.
//    091025 Replaced 'Parser.Exception' by 'EvalError'.
//    091031 Moved definition of Ignore to Factor.
//    091101 'insertAlph' replaced by simple loop.
//
//  Version 1.88.J02.
//    110314 Bug fix in 'root': did not accept product of n-th powers
//           because of an extra increment of 'i'.
//
//  Version 1.89.J01.
//    120209 Definition of Ignore moved to separate file:
//           replaced 'Factor.Ignore' by 'Ignore'.
//    120209 Method 'isCompatibleWith' renamed to 'hasSameFactorsAs'
//           to avoid confusion with method defined in Value.
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
  /** The factors in a Product are represented as element of this Vector.
   *  The factors are Factor objects (units or prefixes).
   *  They are sorted in increasing ALPHABETIC ORDER of their names.
   *  Duplicates are allowed (mean a power>1 of the unit). */
  //-------------------------------------------------------------------
  private Vector<Factor> factors = new Vector<Factor>();

  //=====================================================================
  //  Default constructor.
  //=====================================================================
  /**
   *  Constructs empty Product.
   */
  Product()
    {}


  //=====================================================================
  //  Copy constructor.
  //=====================================================================
  /**
   *  Constructs a copy of given Product.
   *
   *  @param p Product to copy.
   */
  Product(final Product p)
    {
      for (Factor f: p.factors)
        factors.add(f);
    }


  //=====================================================================
  //  add Factor
  //=====================================================================
  /**
   *  Adds given Factor to this Product.
   *  (Originally 'addsubunit').
   *
   *  @param  f Factor to be added.
   *  @return the modified Product.
   */
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
  //  add Product
  //=====================================================================
  /**
   *  Adds all factors of a given Product to this Product.
   *  (Originally 'addsubunitlist').
   *
   *  @param  p Product to be added.
   *  @return the modified Product.
   */
  Product add(final Product p)
    {
      for (int i=0;i<p.size();i++)
        add(p.factor(i));
      return this;
    }


  //=====================================================================
  // size
  //=====================================================================
  /**
   *  Obtains number of factors in this Product.
   *
   *  @return number of factors.
   */
  public int size()
    { return factors.size(); }


  //=====================================================================
  //  factor
  //=====================================================================
  /**
   *  Obtains a Factor of this Product with given index.
   *
   *  @param  i the index
   *  @return the i-th Factor of this Product.
   */
  public Factor factor(int i)
    { return factors.elementAt(i); }


  //=====================================================================
  //  delete
  //=====================================================================
  /**
   *  Removes a Factor of this Product with given index.
   *
   *  @param  i the index
   */
  public void delete(int i)
    { factors.removeElementAt(i); }

  public Vector<Factor> getFactors() {
    return factors;
  }

  //=====================================================================
  //  hasSameFactors
  //=====================================================================
  /**
   *  Compares this Products with another Product.
   *  (Originally 'compareproducts'.)
   *
   *  @param  p Product to compare with.
   *  @param  ignore indicates which Factors should be ignored.
   *  @return true if this Product and Product 'p' have the same
   *          factors, except those to be ignored.
   *          Otherwise false.
   */
  boolean hasSameFactorsAs(final Product p, Ignore ignore)
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
  //  asString
  //=====================================================================
  /**
   *  Represents this Product as a String.
   *
   *  @return printable representation of this Product.
   */
  public String asString()
    {
      StringBuffer sb = new StringBuffer();
      int counter = 1;

      for (int i=0;i<size();i++)
      {
        Factor f = factor(i);

        //-------------------------------------------------------------
        // If s is the same as preceding, increment counter.
        //-------------------------------------------------------------
        if (i>0 && f==factor(i-1))
          counter++;

        //-------------------------------------------------------------
        // If s is first or distinct from preceding:
        //-------------------------------------------------------------
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
  //  root
  //=====================================================================
  /**
   *  Computes a root of this Product.
   *  (Originally 'subunitroot').
   *
   *  @param  n positive integer.
   *  @return n-th root of this Product, or null if this Product
   *          is not an n-th power.
   */
  Product root(int n)
    {
      Product p = new Product();
      int i = 0;
      while (i<size())
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
}
