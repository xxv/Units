/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.buttercookie.units;

import static net.sourceforge.unitsinjava.UnitList.RoundMode.INTEGER;
import static net.sourceforge.unitsinjava.UnitList.RoundMode.SEPARATED_FRACTION;

import android.content.Context;

import net.sourceforge.unitsinjava.Env;
import net.sourceforge.unitsinjava.EvalError;
import net.sourceforge.unitsinjava.Factor;
import net.sourceforge.unitsinjava.Function;
import net.sourceforge.unitsinjava.Ignore;
import net.sourceforge.unitsinjava.UnitList;
import net.sourceforge.unitsinjava.Value;

/**
 * A wrapper of Value to allow for GUI interactions by getting
 * rid of the input/output calls.
 *
 * @author steve
 */
public class ValueGui extends Value {
    //=====================================================================

    /**
     * Constructs a completely reduced Value from unit expression;
     * throws exception on error.
     * <br>
     * If the Value cannot be constructed because of incorrect syntax,
     * unknown unit name, etc., throws an exception.
     * <br>(Originally 'processunit'.)
     *
     * @param s a unit expression.
     * @return Value represented by the expression,
     * or null if the Value could not be constructed.
     */
    //=====================================================================
    public static Value fromString(final String s) throws EvalError {
        final Value v = parse(s);
        v.completereduce();
        return v;
    }

    public static Value fromUnicodeString(final String s) throws EvalError {
        // closeParens is run twice in order to allow for unicode characters mapping to functions.
        final Value v = parse(closeParens(Units.unicodeToAscii(s)));
        v.completereduce();
        return v;
    }

    /**
     * Close any open parentheses.
     *
     * @param s
     * @return
     */
    public static String closeParens(String s) {
        final StringBuilder sb = new StringBuilder(s);
        final int len = s.length();
        int openParen = 0;
        int closedParen = 0;
        for (int i = 0; i < len; i++) {
            final char ch = s.charAt(i);
            if (ch == '(') {
                openParen++;
            } else if (ch == ')') {
                closedParen++;
            }
        }
        if (openParen > closedParen) {
            for (int i = 0; i < openParen - closedParen; i++) {
                sb.append(')');
            }
        }
        return sb.toString();
    }

    public static Value getReciprocal(Value inval) {
        final Value inv = new Value();
        inv.factor = 1 / inval.factor;
        inv.numerator = inval.denominator;
        inv.denominator = inval.numerator;

        return inv;
    }

    public static String getFingerprint(Value val) {
        final StringBuilder unitFprint = new StringBuilder();
        for (final Factor f : val.numerator.getFactors()) {
            unitFprint.append(f.name);
            unitFprint.append(',');
        }
        unitFprint.append(';');
        for (final Factor f : val.denominator.getFactors()) {
            unitFprint.append(f.name);
            unitFprint.append(',');
        }
        return unitFprint.toString();
    }

    public static String getFingerprint(UnitList ul) {
        return getFingerprint(ul.value[0]);
    }

    //=====================================================================
    //  convert to Value
    //=====================================================================

    /**
     * Shows result of conversion of unit expression to unit expression.
     *
     * @param fromValue 'from' expression converted to completely reduced Value.
     * @param toValue   'to' expression converted to completely reduced Value.
     * @throws ConversionException
     */
    public static double convertNonInteractive
    (Value fromValue, Value toValue) throws ConversionException {
        //---------------------------------------------------------------
        //  If 'toValue' and 'fromValue' are not compatible,
        //  we may be doing reciprocal conversion.
        //---------------------------------------------------------------
        if (!fromValue.isCompatibleWith(toValue, Ignore.DIMLESS)) {
            final Value invfrom = getReciprocal(fromValue);    // inverse of fromValue, if needed

            //-------------------------------------------------------------
            //  If reciprocal conversion not wanted, or inverse of 'fromValue'
            //  is not compatible with 'toValue', we have conformability error.
            //-------------------------------------------------------------
            if (Env.strict || !toValue.isCompatibleWith(invfrom, Ignore.DIMLESS)) {
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
     * Returns result of conversion of unit expression to function.
     *
     * @param fromValue 'from' expression converted to completely reduced Value.
     * @param fun       'to' function.
     */
    public static String convertNonInteractive(Value fromValue, Function fun) throws ConversionException {
        try {
            fun.applyInverseTo(fromValue);
            fromValue.completereduce();
        } catch (final EvalError e) {
            final ConversionException ce = new ConversionException(e.getMessage());
            ce.initCause(e);
            throw ce;
        }
        return fun.name + "(" + fromValue.asString() + ")";
    }

    //=====================================================================
    //  convert a list of units
    //=====================================================================

    /**
     * Returns result of conversion of unit expression to a list of units.
     *
     * @param fromExpr  The original user-entered 'from' expression
     * @param fromValue 'from' expression converted to completely reduced Value.
     * @param ul       'to' list of units.
     */
    public static String convertNonInteractive(Context context, String fromExpr, Value fromValue, UnitList ul) throws ConversionException {
        boolean result = ul.convert(fromExpr, fromValue);
        if (!result) {
            throw new ConversionException();
        } else {
            StringBuilder sb = new StringBuilder();
            boolean printedSomething = false;
            String sep = "";
            for (int i = 0; i < ul.n; i++) {
                if (ul.result[i] != 0 ||
                        // If everything's zero, always print the last partial result, even if that
                        // might be zero, too.
                        i == ul.n - 1 && !printedSomething && ul.roundMode != SEPARATED_FRACTION) {
                    sb.append(sep);
                    sb.append(UnitList.showUnit(ul.result[i], ul.unit[i]));
                    sep = " + ";
                    printedSomething = true;
                }
            }
            if (ul.roundMode == SEPARATED_FRACTION &&
                    (ul.rounded != 0 || !printedSomething)) {
                sb.append(sep);
                sb.append(UnitList.showUnit(ul.rounded, ul.unit[ul.n - 1]));
            } else if (ul.roundMode == INTEGER && ul.roundAmount != 0) {
                sb.append(" (");
                if (ul.roundAmount > 0) {
                    sb.append(context.getString(R.string.result_rounded_up, ul.unit[ul.n - 1]));
                } else {
                    sb.append(context.getString(R.string.result_rounded_down, ul.unit[ul.n - 1]));
                }
                sb.append(")");
            }
            return sb.toString();
        }
    }

    public static class ConversionException extends Exception {
        /**
         *
         */
        private static final long serialVersionUID = 834962768736410424L;

        public ConversionException(String msg) {
            super(msg);
        }

        public ConversionException() {
            super();
        }
    }

    public static class ReciprocalException extends ConversionException {

        /**
         *
         */
        private static final long serialVersionUID = 5809033194217476893L;
        public final Value reciprocal;

        public ReciprocalException(Value reciprocal) {
            super();
            this.reciprocal = reciprocal;
        }
    }
}
