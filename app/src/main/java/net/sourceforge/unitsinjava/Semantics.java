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
//    091024 Created for Version 1.87.J01.
//
//=========================================================================

package net.sourceforge.unitsinjava;

/**
 *  Holds procedures called by Parser to process unit expressions.
 */

 public class Semantics extends net.sourceforge.unitsinjava.SemanticsBase
{
  //=====================================================================
  //
  //  Parameter and result
  //
  //=====================================================================

  String parm = null;
  Value parmValue;
  Value result;

  //=====================================================================
  //
  //  Semantic values
  //
  //=====================================================================
  public static class SV
  {
    double   number;
    Value    value;
    Function func;
  }

  SV lhSem()
    { return sv(lhs()); }

  SV rhSem(int i)
    { return sv(rhs(i)); }

  SV sv(Phrase p)
    {
      SV sv = (SV)(p.get());
      if (sv!=null) return sv;
      sv = new SV();
      p.put(sv);
      return sv;
    }

  //=====================================================================
  //
  //  Semantic actions
  //
  //=====================================================================
  //-------------------------------------------------------------------
  //  unitexpr = space? expr? EOT
  //               0      1    2
  //-------------------------------------------------------------------
  void unitexpr()
    {
      if (rhsSize()==3)
        result = rhSem(1).value;
      else
        result = new Value();
    }

  //-------------------------------------------------------------------
  //  failed unitexpr = space? expr? EOT
  //-------------------------------------------------------------------
  void error()
    { throw new EvalError(lhs().errMsg()); }

  //-------------------------------------------------------------------
  //  expr = term ((PLUS | MINUS) term)*
  //           0        1,3,..    2,4,..
  //-------------------------------------------------------------------
  void expr()
    {
      Value v = rhSem(0).value;
      for (int i=2;i<rhsSize();i+=2)
      {
        Value v1 = rhSem(i).value;
        if (rhs(i-1).isA("MINUS"))
          v1.factor *= -1;
        v.add(v1);
      }
      lhSem().value = v;
    }

  //-------------------------------------------------------------------
  //  expr = SLASH product
  //           0      1
  //-------------------------------------------------------------------
  void inverse()
    {
      Value v = rhSem(1).value;
      v.invert();
      lhSem().value = v;
    }

  //-------------------------------------------------------------------
  //  term = product ((STAR / SLASH / PER) product)*
  //            0           1,3,..        2,4,..
  //-------------------------------------------------------------------
  void term()
    {
      Value v = rhSem(0).value;
      for (int i=2;i<rhsSize();i+=2)
      {
        if (rhs(i-1).isA("STAR"))
          v.mult(rhSem(i).value);
        else
          v.div(rhSem(i).value);
      }
      lhSem().value = v;
    }

  //-------------------------------------------------------------------
  //  product = factor factor*
  //               0    1,2,..
  //-------------------------------------------------------------------
  void product()
    {
      Value v = rhSem(0).value;
      for (int i=1;i<rhsSize();i++)
        v.mult(rhSem(i).value);
      lhSem().value = v;
    }

  //-------------------------------------------------------------------
  //  factor = unary ((HAT / STARSTAR) unary)*
  //             0        1,3,,,       2,4,..
  //-------------------------------------------------------------------
  void factor()
    {
      for (int i=rhsSize()-3;i>=0;i-=2)
        rhSem(i).value.power(rhSem(i+2).value);

      lhSem().value = rhSem(0).value;
    }

  //-------------------------------------------------------------------
  //  unary = (PLUS /MINUS) primary
  //               0          0/1
  //-------------------------------------------------------------------
  void unary()
    {
      Value v = rhSem(rhsSize()-1).value;
      if (rhs(0).isA("MINUS"))
        v.factor *= -1;
      lhSem().value = v;
    }

  //-------------------------------------------------------------------
  //  primary = numexpr
  //               0
  //-------------------------------------------------------------------
  void makeNumUnit()
    {
      Value v = new Value();
      v.factor = rhSem(0).number;
      lhSem().value = v;
    }

  //-------------------------------------------------------------------
  //  primary  = LPAR expr RPAR
  //               0    1    2
  //-------------------------------------------------------------------
  void pass2()
    { lhSem().value = rhSem(1).value; }

  //-------------------------------------------------------------------
  //  primary = unitname
  //               0
  //-------------------------------------------------------------------
  void pass()
    { lhSem().value = rhSem(0).value; }

  //-------------------------------------------------------------------
  //  primary = bfunc LPAR expr RPAR
  //              0     1    2    3
  //-------------------------------------------------------------------
  void evalBfunc()
    {
      lhSem().value = rhSem(2).value;
      rhSem(0).func.applyTo(lhSem().value);
    }

  //-------------------------------------------------------------------
  //  primary = opttilde ufunc LPAR expr RPAR
  //                0      1     2    3    4
  //-------------------------------------------------------------------
  void evalUfunc()
    {
      lhSem().value = rhSem(3).value;
      if (rhs(0).isEmpty())
        rhSem(1).func.applyTo(lhSem().value);
      else
        rhSem(1).func.applyInverseTo(lhSem().value);
    }

  //-------------------------------------------------------------------
  //  numexpr = number (BAR   number)*
  //               0    1,3,.. 2,4,..
  //-------------------------------------------------------------------
  void numexpr()
    {
      Double d = rhSem(0).number;
      for (int i=2;i<rhsSize();i+=2)
      {
        double d1 = rhSem(i).number;
        if (d1==0) throw new EvalError("Division by 0");
        d /= d1;
      }
      lhSem().number = d;
    }

  //-------------------------------------------------------------------
  //  number = mantissa exponent? space?
  //               1       2       2/3
  //-------------------------------------------------------------------
  void number()
    { lhSem().number = new Double(rhsText(0,rhsSize()-1)); }

  //-------------------------------------------------------------------
  //  unitname = word space?
  //               0    1
  //-------------------------------------------------------------------
  boolean unitname()
    {
      String word = rhs(0).text();
      if (word.equals("per")) return false;

      if (BuiltInFunction.table.containsKey(word)) return false;

      if (word.equals(parm))
      {
        lhSem().value = new Value(parmValue);
        return true;
      }

      if (DefinedFunction.table.containsKey(word)) return false;

      // Do exponent handling like m3
      int exp = 2 + "23456789".indexOf(word.charAt(word.length()-1));
      if (exp>1)
        word = word.substring(0,word.length()-1);

      Value v = Value.fromName(word);

      if (exp>1) v.power(exp);

      lhSem().value = v;

      return true;
    }

  //-------------------------------------------------------------------
  //  bfunc = word space?
  //            0    1
  //-------------------------------------------------------------------
  boolean bfunc()
    {
      String word = rhs(0).text();
      if (word.equals("per")) return false;
      Function func = BuiltInFunction.table.get(word);
      if (func==null) return false;
      lhSem().func = func;
      return true;
    }

  //-------------------------------------------------------------------
  //  ufunc = word space?
  //            0    1
  //-------------------------------------------------------------------
  boolean ufunc()
    {
      String word = rhs(0).text();
      if (word.equals("per")) return false;
      if (word.equals(parm)) return false;
      Function func = DefinedFunction.table.get(word);
      if (func==null) return false;
      lhSem().func = func;
      return true;
    }

  //-------------------------------------------------------------------
  //  space = [ \t]*
  //-------------------------------------------------------------------
  void space()
    { lhs().errClear(); }

}
