package info.staticfree.android.units;

import net.sourceforge.unitsinjava.Env;
import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Factor;
import net.sourceforge.unitsinjava.Function;
import net.sourceforge.unitsinjava.Value;

/**
 * A wrapper of Value to allow for GUI interactions by getting
 * rid of the input/output calls.
 *
 * @author steve
 *
 */
public class ValueGui extends Value {
	  //=====================================================================
	  /** Constructs a completely reduced Value from unit expression;
	   *  throws exception on error.
	   *  <br>
	   *  If the Value cannot be constructed because of incorrect syntax,
	   *  unknown unit name, etc., throws an exception.
	   *  <br>(Originally 'processunit'.)
	   *  @param  s a unit expression.
	   *  @return Value represented by the expression,
	   *          or null if the Value could not be constructed. */
	  //=====================================================================
	  public static Value fromString(final String s) throws EvalError
	    {
	        final Value v = parse(s);
	        v.completereduce();
	        return v;
	    }

	  public static Value fromUnicodeString(final String s) throws EvalError
	    {
		  // closeParens is run twice in order to allow for unicode characters mapping to functions.
	        final Value v = parse(closeParens(Units.unicodeToAscii(s)));
	        v.completereduce();
	        return v;
	    }

	/**
	 * Close any open parentheses.
	 * @param s
	 * @return
	 */
	public static String closeParens(String s){
		  final StringBuilder sb = new StringBuilder(s);
		  final int len = s.length();
		  int openParen = 0;
		  int closedParen = 0;
		  for (int i = 0; i < len; i++){
			  final char ch = s.charAt(i);
			  if (ch == '('){
				  openParen++;
			  }else if (ch == ')'){
				  closedParen++;
			  }
		  }
		  if (openParen > closedParen){
			  for (int i = 0; i < openParen - closedParen; i++){
				  sb.append(')');
			  }
		  }
		  return sb.toString();
	  }

	  public static Value getReciprocal(Value inval){
		    final Value inv = new Value();
		    inv.factor = 1/inval.factor;
	        inv.numerator = inval.denominator;
	        inv.denominator = inval.numerator;

	        return inv;
	  }

	  public static String getFingerprint(Value val){
		   final StringBuilder unitFprint = new StringBuilder();
		   for (final Factor f : val.numerator.getFactors()){
			   unitFprint.append(f.name);
			   unitFprint.append(',');
		   }
		   unitFprint.append(';');
		   for (final Factor f : val.denominator.getFactors()){
			   unitFprint.append(f.name);
			   unitFprint.append(',');
		   }
		   return unitFprint.toString();
	  }

	  //=====================================================================
	  //  convert to Value
	  //=====================================================================
	  /**
	   *  Shows result of conversion of unit expression to unit expression.
	   *
	   *  @param  fromValue 'from' expression converted to completely reduced Value.
	   *  @param  toValue 'to' expression converted to completely reduced Value.
	 * @throws ConversionException
	   */
	  public static double convertNonInteractive
	    (Value fromValue, Value toValue) throws ConversionException
	    {
	      //---------------------------------------------------------------
	      //  If 'toValue' and 'fromValue' are not compatible,
	      //  we may be doing reciprocal conversion.
	      //---------------------------------------------------------------
	      if (!fromValue.isCompatibleWith(toValue,Factor.Ignore.DIMLESS))
	      {
	    	   final Value invfrom = getReciprocal(fromValue);    // inverse of fromValue, if needed

	        //-------------------------------------------------------------
	        //  If reciprocal conversion not wanted, or inverse of 'fromValue'
	        //  is not compatible with 'toValue', we have conformability error.
	        //-------------------------------------------------------------
	        if (Env.strict || !toValue.isCompatibleWith(invfrom,Factor.Ignore.DIMLESS))
	        {
	        	throw new ConversionException();
	        }

	        //-------------------------------------------------------------
	        //  We arrive here to do a reciprocal conversion.
	        //-------------------------------------------------------------
	        throw new ReciprocalException(invfrom);
	      }

	      return fromValue.factor / toValue.factor;
	    }

	  //=====================================================================
	  //  convert to Function
	  //=====================================================================
	  /**
	   *  Returns result of conversion of unit expression to function.
	   *
	   *  @param  fromExpr 'from' expression.
	   *  @param  fromValue 'from' expression converted to completely reduced Value.
	   *  @param  fun 'to' function.
	   */
	  public static String convertNonInteractive (Value fromValue, Function fun) throws ConversionException {
	      try {
	        fun.applyInverseTo(fromValue);
	        fromValue.completereduce();
	      }
	      catch(final EvalError e)
	      {
	    	  final ConversionException ce = new ConversionException(e.getMessage());
	    	  ce.initCause(e);
	    	  throw ce;
	      }
	      return fun.name + "(" + fromValue.asString() + ")";
	    }


	  public static class ConversionException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 834962768736410424L;

		public ConversionException(String msg){
			  super(msg);
		  }

		public ConversionException(){
			super();
		}
	  }

	  public static class ReciprocalException extends ConversionException {

			/**
		 *
		 */
		private static final long serialVersionUID = 5809033194217476893L;
		public Value reciprocal;
			public ReciprocalException(Value reciprocal){
				  super();
				  this.reciprocal = reciprocal;
			  }
	  }
}
