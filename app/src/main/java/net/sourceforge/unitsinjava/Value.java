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
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//-------------------------------------------------------------------------
//
//  Change log
//
//    050315 Version 1.84.J07. Changed package name to "units".
//    061230 Version 1.86.J01.
//           Modified 'convert' methods to apply new options.
//    091025 Version 1.87.J01.
//           Replaced 'Parser.Exception' by 'EvalError'.
//           Used the new Parser in 'parse'.
//    091027 Corrected handling of non-integer power of negative
//           numbers in 'power' and 'root' (Math.pow produces NaN).
//    091031 Moved definition of Ignore to Factor.
//
//=========================================================================

package net.sourceforge.unitsinjava;


//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
//
//  class Value
//
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH
/**
 *  A number multiplied by a dimension.
 *  <br>
 *  The number is called <i>factor</i>.
 *  The dimension is a combination of units, represented as a quotient
 *  of <i>numerator</i> and <i>denominator</i>:
 *
 *  <pre><code>   Value = factor * (numerator / denominator)</code></pre>
 *
 *  The numerator ane denominator are products of units.
 *  Each is represented by a Product object.
 *  <p>
 *  A Value is <i>reduced</i> if all units appearing in its
 *  numerator and denominator are primitive, and these products do not
 *  contain common factors.
 *  A Value represents a number if its numerator and denominator
 *  are both empty.
 *  <p>
 *  The class has methods for arithmetic operations on Values,
 *  reduction to primitive units, copying, and printing out Values.
 *  In addition to constructors, there are static methods to construct
 *  Values from unit names and unit expressions.
 */
//HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH

 public class Value
{
  //-------------------------------------------------------------------
  //  Components of a Value
  //-------------------------------------------------------------------
  public double factor;
  public Product numerator;
  public Product denominator;

  //-------------------------------------------------------------------
  /** Semantics of unit expressions.
   *  A static object containing semantic procedures for parser
   *  of unit expressions. It is used by the two 'parse' methods. */
  //-------------------------------------------------------------------
  private static Semantics unitSem = new Semantics();


  //=====================================================================
  //  Default constructor
  //=====================================================================
  /** Constructs a Value representing dimensionless number 1.
   *  <br>(Originally 'initializeunit'.)
   */
  public Value()
    {
      factor = 1.0;
      numerator   = new Product();
      denominator = new Product();
    }


  //=====================================================================
  /** Constructs copy of a given Value.
   *  @param  v a Value to be copied. */
  //=====================================================================
  public Value(final Value v)
    {
      factor = v.factor;
      numerator   = new Product(v.numerator);
      denominator = new Product(v.denominator);
    }


  //=====================================================================
  /** Makes this Value a copy of given Value.
   *  @param  v the Value to be copied.
   *  @return this Value - now a copy of 'v'. */
  //=====================================================================
  Value copyFrom(final Value v)
    {
      factor = v.factor;
      numerator   = new Product(v.numerator);
      denominator = new Product(v.denominator);
      return this;
    }


  //=====================================================================
  /** Constructs a Value from unit expression;
   *  throws exception on error.
   *  <br>
   *  EvalError is thrown if the Value cannot be constructed
   *  because of incorrect syntax, unknown unit name, etc..
   *  The exception contains a complete error message.
   *  <br>(Originally 'parseunit'.)
   *  @param  s a unit expression.
   *  @return Value represented by the expression. */
  //=====================================================================
  public static Value parse(final String s)
    {
      Parser parser = new Parser();           // Instantiate Parser + Semantics
      Semantics sem = parser.semantics();     // Access Semantics
      SourceString src = new SourceString(s); // Wrap 's' for parser
      parser.parse(src);                      // Parse 's' - EvalError on failure
      return sem.result;                      // Obtain result from Semantics
    }


  //=====================================================================
  /** Constructs a Value from unit expression,
   *  substituting given Value for a parameter;
   *  throws exception on error.
   *  <br>
   *  The unit expression 's' may contain the string given as 'parm'
   *  at a place that syntactically corresponds to a unit name.
   *  This string is then treated as name of Value given as 'parmValue',
   *  rather than a unit name.
   *  <br>
   *  EvalError is thrown if the Value cannot be constructed
   *  because of incorrect syntax, unknown unit name, etc..
   *  The exception contains a complete error message.
   *  @param  s a unit expression.
   *  @param  parm parameter name.
   *  @param  parmValue Value to be substituted for 'parm'.
   *  @return Value represented by the expression. */
  //=====================================================================
  public static Value parse(final String s, final String parm, final Value parmValue)
    {
      Parser parser = new Parser();           // Instantiate Parser + Semantics
      Semantics sem = parser.semantics();     // Access Semantics
      sem.parm = parm;                        // Identify parameter to replace
      sem.parmValue = parmValue;              // Supply parameter value
      SourceString src = new SourceString(s); // Wrap 's' for parser
      parser.parse(src);                      // Parse 's' - EvalError on failure
      return sem.result;                      // Obtain result from Semantics
    }


  //=====================================================================
  /** Constructs a completely reduced Value from unit expression;
   *  writes mesage on error.
   *  <br>
   *  If the Value cannot be constructed because of incorrect syntax,
   *  unknown unit name, etc., writes an error message and returns null.
   *  <br>(Originally 'processunit'.)
   *  @param  s a unit expression.
   *  @return Value represented by the expression,
   *          or null if the Value could not be constructed. */
  //=====================================================================
  public static Value fromString(final String s)
    {
      try
      {
        Value v = parse(s);
        v.completereduce();
        return v;
      }
      catch (EvalError e)
      {
        Env.err.println(e.getMessage());
        return null;
      }
    }


  //=====================================================================
  /** Constructs a Value from a string that may be name of a unit
   *  or a prefix, or a prefixed unit name, possibly in plural from.
   *  Throws exception if the string is none of them.
   *  @param  s possible name of a unit, prefix, or prefixed unit.
   *  @return Value represented by 's'. */
  //=====================================================================
  public static Value fromName(final String s)
    {
      Factor[] pu = Factor.split(s);
      if (pu==null)
        throw new EvalError("Unit '" + s + "' is unknown.");

      Value v = new Value();

      if (pu[0]!=null)
        v.numerator.add(pu[0]);

      if (pu[1]!=null)
        v.numerator.add(pu[1]);

      return v;
    }


  //=====================================================================
  /** Constructs printable string representing this Value.
   *  @return this Value as printable string. */
  //=====================================================================
  public String asString()
    {
      StringBuffer sb = new StringBuffer();

      sb.append(Util.shownumber(factor)).append(numerator.asString());

      if (denominator.size()>0)
        sb.append(" /").append(denominator.asString());

      return sb.toString();
    }


  //=====================================================================
  /** Prints out this Value.
   *  <br>(Originally 'showunit'.) */
  //=====================================================================
  void show()
    { Env.out.println(asString()); }


  //=====================================================================
  /** Checks if this Value is compatible with another Value.
   *  <br>
   *  Two Values are compatible if they have compatible
   *  numerators and denominators.
   *  <br>(Originally 'compareunits'.)
   *  @param  v Value to be checked against.
   *  @return <code>true</code> if the Values are compatible, or
   *          <code>false</code> otherwise. */
  //=====================================================================
  public boolean isCompatibleWith(final Value v, Factor.Ignore ignore)
    {
      return numerator.isCompatibleWith(v.numerator,ignore)
           && denominator.isCompatibleWith(v.denominator,ignore);
    }


  //=====================================================================
  /** Reduces this Value and checks if it represents a number.
   *  <br>
   *  A Value represents a number if it is dimensionless, that is,
   *  its numerator and denominator are both empty.
   *  @return <code>true</code> if the Value represents a number, or
   *          <code>false</code> otherwise. */
  //=====================================================================
  boolean isNumber()
    {
      completereduce();
      return numerator.size()==0 && denominator.size()==0;
    }


  //=====================================================================
  /** Adds given Value to this Value.
   *  <br>(Originally 'addunit'.)
   *  @param  v Value to be added. */
  //=====================================================================
  void add(final Value v)
    {
      completereduce();
      v.completereduce();
      if (!isCompatibleWith(v,Factor.Ignore.NONE))
        throw new EvalError("Sum of non-conformable values:\n\t"
                          + asString() + "\n\t" + v.asString() + ".");
      factor += v.factor;
    }


  //=====================================================================
  /** Multiplies this Value by a given Value.
   *  <br>(Originally 'multunit'.)
   *  @param  v Value to multiply by. */
  //=====================================================================
  void mult(final Value v)
    {
      factor *= v.factor;
      numerator.add(v.numerator);
      denominator.add(v.denominator);
    }


  //=====================================================================
  /** Divide this Value by a given Value.
   *  <br>(Originally 'divunit'.)
   *  @param  v Value to divide by. */
  //=====================================================================
  void div(final Value v)
    {
      if (v.factor==0)
        throw new EvalError("Division of " + asString()
                          + " by zero (" + v.asString() + ").");
      factor /= v.factor;
      denominator.add(v.numerator);
      numerator.add(v.denominator);
    }


  //=====================================================================
  /** Inverts this Value.
   *  <br>(Originally 'invertunit'.) */
  //=====================================================================
  void invert()
    {
      if (factor==0)
        throw new EvalError("Division by zero (" + asString() + ").");
      factor = 1.0/factor;
      Product num = numerator;
      numerator = denominator;
      denominator = num;
    }


  //=====================================================================
  /** Raises this Value to power specified by another Value.
   *  The Value supplied as exponent must represent a number.
   *  If that number is not an integer or a fraction 1/integer,
   *  this Value must represent a number.
   *  <br>(Originally 'unitpower'.)
   *  @param  v the exponent. */
  //=====================================================================
  void power(final Value v)
    {
      //---------------------------------------------------------------
      //  Exponent must a number.
      //---------------------------------------------------------------
      if (!v.isNumber())
        throw new EvalError("Non-numeric exponent, " + v.asString()
                          + ", of " + asString() + ".");

      //---------------------------------------------------------------
      //  Get numeric value of the exponent.
      //---------------------------------------------------------------
      double p = v.factor;

      //---------------------------------------------------------------
      //  Apply absolute value of the exponent.
      //---------------------------------------------------------------
      if (Math.floor(p)==p)         // integer exponent
        power((int)Math.abs(p));

      else
      if (Math.floor(1.0/p)==1.0/p) // fractional exponent
        root((int)Math.abs(1.0/p));

      else                          // exponent neither n nor 1/n
      {                             // .. for integer n..
        if (!isNumber())
          throw new EvalError("Non-numeric base, " + asString()
                            + ", for exponent " + v.asString() + ".");

        Double f = Math.pow(factor,Math.abs(p));

        if (Double.isNaN(f))
          throw new EvalError("The result of " + factor + "^" + p
                            + " is undefined.");
        factor = f;
      }

      //---------------------------------------------------------------
      //  Invert if exponent was negative.
      //---------------------------------------------------------------
      if (p<0) invert();
    }


  //=====================================================================
  /** Raises this Value to integer power n>=0.
   *  <br>(Originally 'expunit').
   *  @param  n the exponent. */
  //=====================================================================
  void power(int n)
    {
      if (n<0)
        throw new Error("Program error: exponent " + n + ".");

      Product num = new Product();
      Product den = new Product();
      double fac = 1.0;

      for (int i=0;i<n;i++)
      {
        fac *= factor;
        num.add(numerator);
        den.add(denominator);
      }

      factor = fac;
      numerator = num;
      denominator = den;
    }


  //=====================================================================
  /** Computes the n-th root of this Value for an integer n.
   *  The integer n must not be 0.
   *  If n is even, this Value must be non-negative.
   *  <br>(Originally 'rootunit'.)
   *  @param  n the exponent. */
  //=====================================================================
  void root(int n)
    {
      if (n==0 || (n%2==0 && factor<0))
        throw new EvalError("Illegal n-th root of " + asString()
                          +  ", n=" + n + ".");

      completereduce();
      Product num = numerator.root(n);
      Product den = denominator.root(n);
      if (num==null || den==null)
        {
          String nth = n==2? "square" : (n==3? "cube" : n + "-th");
          throw new EvalError(asString() + " is not a " + nth + " root.");
        }

      numerator = num;
      denominator = den;

      // Math.pow does not work for negative base and non-integer exponent,
      // so negative 'factor' must be treated separately.

      if (factor>=0)
        factor = Math.pow(factor,1.0/(double)n);
      else
        factor = -Math.pow(-factor,1.0/(double)n);
    }


  //=====================================================================
  /** Removes factors that appear in both the numerator and denominator.
   *  <br>(Originally 'cancelunit'.) */
  //=====================================================================
  void cancel()
    {
      int den = 0;
      int num = 0;

      while (num<numerator.size() && den<denominator.size())
      {
        int comp = (denominator.factor(den).name).
                compareTo(numerator.factor(num).name);
        if (comp==0)
        {                    // units match, so cancel them.
          denominator.delete(den);
          numerator.delete(num);
        }
        else if (comp < 0)   // Move up whichever index is alphabetically..
          den++;             // ..behind to look for future matches.
        else
          num++;
      }

    }


  //=====================================================================
  /** Reduces numerator or denominator of this Value to primitive units.
   *  @param  flip indicates whether to reduce the numerator
   *          (<code>flip = false</code>) or denominator
   *          (<code>flip = true</code>).
   *  @return <code>true</code> if reduction was performed, or
   *          <code>false</code> if there is nothing more to reduce. */
  //=====================================================================
  boolean reduceproduct(boolean flip)
    {
      boolean didsomething = false;
      Product prod = (flip? denominator : numerator);
      if (flip)
        denominator = new Product();
      else
        numerator = new Product();
      Product newprod = (flip? denominator : numerator);

      //---------------------------------------------------------------
      //  Process all factors of 'prod'
      //---------------------------------------------------------------
      for (int f=0;f<prod.size();f++)
      {
        Factor fact = prod.factor(f);
        String toadd = fact.name;
        if (toadd==null)                                   // Is this possible?
          throw new EvalError("Unit '" + toadd + "' is unknown.");

        if (fact.isPrimitive)
        {
          newprod.add(fact);
          continue;
        }

        Value newval;

        try
        { newval = parse(fact.def); }

        catch (EvalError e)
        {
          throw new EvalError("Invalid definition of '" + toadd + "'. "
                            + e.getMessage());
        }

        if (flip) div(newval);
        else mult(newval);

        didsomething = true;

      } // end process all factors

      return didsomething;
    }


  //=====================================================================
  /** Reduces this Value as much as possible. */
  //=====================================================================
  public void completereduce()
    {
      /* Keep calling reduceproduct until it doesn't do anything */
      while (true)
      {
        boolean topchanged = reduceproduct(false);
        boolean botchanged = reduceproduct(true);
        if (!topchanged && !botchanged) break;
      }
      cancel();
    }


  //=====================================================================
  //  convert to Value
  //=====================================================================
  /**
   *  Shows result of conversion of unit expression to unit expression.
   *
   *  @param  fromExpr 'from' expression.
   *  @param  fromValue 'from' expression converted to completely reduced Value.
   *  @param  toExpr 'to' expression.
   *  @param  toValue 'to' expression converted to completely reduced Value.
   */
  public static boolean convert
    ( String fromExpr, Value fromValue,
      String toExpr, Value toValue)
    {
      Value invfrom = new Value();    // inverse of fromValue, if needed
      boolean doingrec;               // reciprocal conversion?

      doingrec = false;
      fromExpr = fromExpr.trim();
      toExpr = toExpr.trim();

      //---------------------------------------------------------------
      //  If 'toValue' and 'fromValue' are not compatible,
      //  we may be doing reciprocal conversion.
      //---------------------------------------------------------------
      if (!fromValue.isCompatibleWith(toValue,Factor.Ignore.DIMLESS))
      {
        //-------------------------------------------------------------
        //  Construct inverse of 'fromValue' in 'invfrom'.
        //-------------------------------------------------------------
        invfrom.factor = 1/fromValue.factor;
        invfrom.numerator = fromValue.denominator;
        invfrom.denominator = fromValue.numerator;

        //-------------------------------------------------------------
        //  If reciprocal conversion not wanted, or inverse of 'fromValue'
        //  is not compatible with 'toValue', we have conformability error.
        //-------------------------------------------------------------
        if (Env.strict || !toValue.isCompatibleWith(invfrom,Factor.Ignore.DIMLESS))
        {
          Env.err.println("conformability error");
          Env.err.print(Env.verbose==2? "\t" + fromExpr + " = " : Env.verbose==1? "\t" : "");
          fromValue.show();
          Env.err.print(Env.verbose==2? "\t" + toExpr   + " = " : Env.verbose==1? "\t" : "");
          toValue.show();
          return false;
        }

        //-------------------------------------------------------------
        //  We arrive here to do a reciprocal conversion.
        //-------------------------------------------------------------
        Env.out.println("\treciprocal conversion");
        fromValue = invfrom;
        doingrec = true;
      }

      //---------------------------------------------------------------
      //  We arrive here to do conversion, and 'fromValue' is compatible
      //  with 'toValue'. (If conversion is reciprocal, 'fromValue' is already
      //  inverted, and 'doingrec' is true.)
      //  Print the first line of output.
      //---------------------------------------------------------------
      String sep = "";
      String right = "";
      String left = "";

      if (Env.verbose==2)
      {
        if ("0123456789".indexOf(toExpr.charAt(0))>=0)
          sep=" *";
        if (doingrec)
        {
          if (fromExpr.indexOf('/')>=0)
          {
            left="1 / (";
            right=")";
          }
          else
            left="1 / ";
        }
      }

      //---------------------------------------------------------------
      //  Print the first line of output.
      //---------------------------------------------------------------
      if (Env.verbose==2)
        Env.out.print("\t" + left + fromExpr + right + " = ");
      else if (Env.verbose==1)
        Env.out.print("\t* ");

      Env.out.print(Util.shownumber(fromValue.factor / toValue.factor));

      if (Env.verbose==2)
        Env.out.print(sep + " " + toExpr);


      //---------------------------------------------------------------
      //  Print the second line of output.
      //---------------------------------------------------------------
      if (!Env.oneline)
      {
        if (Env.verbose==2)
          Env.out.print("\n\t" + left + fromExpr + right + " = (1 / ");
        else if (Env.verbose==1)
          Env.out.print("\n\t/ ");
        else
          Env.out.print("\n");

        Env.out.print(Util.shownumber(toValue.factor / fromValue.factor));

        if (Env.verbose==2)
          Env.out.print(")" + sep + " " + toExpr);
      }
      Env.out.println("");
      return true;
    }

  //=====================================================================
  //  convert to Function
  //=====================================================================
  /**
   *  Shows result of conversion of unit expression to function.
   *
   *  @param  fromExpr 'from' expression.
   *  @param  fromValue 'from' expression converted to completely reduced Value.
   *  @param  fun 'to' function.
   */
  public static boolean convert
    ( String fromExpr, Value fromValue, Function fun)
    {
      try
      {
        fun.applyInverseTo(fromValue);
        fromValue.completereduce();
      }
      catch(EvalError e)
      {
        Env.out.println(e.getMessage());
        return false;
      }

      if (Env.verbose==2)
        Env.out.print("\t" + fromExpr + " = " + fun.name + "(");
      else
        if (Env.verbose==1) Env.out.print("\t");
      Env.out.print(fromValue.asString());
      if (Env.verbose==2)
        Env.out.print(")");
      Env.out.print("\n");
      return true;
    }


}
