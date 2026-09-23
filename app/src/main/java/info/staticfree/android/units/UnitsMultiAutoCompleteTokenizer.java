package info.staticfree.android.units;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.MultiAutoCompleteTextView;

public class UnitsMultiAutoCompleteTokenizer implements MultiAutoCompleteTextView.Tokenizer {
	final Pattern unitRegex = Pattern.compile("([a-zA-Z]\\w+)$");
	final Pattern unitRegexEnd = Pattern.compile("^([a-zA-Z]\\w+)");
	
	public CharSequence terminateToken(CharSequence text) {
		return text + " ";
	}
	
	public int findTokenStart(CharSequence text, int cursor) {
		final Matcher m = unitRegex.matcher(text.subSequence(0, cursor));
		if (m.find()){
			return m.start();
		}
		return cursor;
	}
	
	public int findTokenEnd(CharSequence text, int cursor) {
		final Matcher m = unitRegexEnd.matcher(text.subSequence(cursor, text.length() - 1));

		if (m.find()){
			return m.end();
		}
		return cursor;
	}
}