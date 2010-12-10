package info.staticfree.android.units;

import net.sourceforge.unitsinjava.Util;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class HistoryEntry implements BaseColumns {
	public final static String
		_HAVE = "have",
		_WANT = "want",
		_RESULT = "result",
		_WHEN = "whenadded";

	public final static String
		PATH = "history";

	public final static Uri
		CONTENT_URI = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH);

	public final static String SORT_DEFAULT = _ID + " ASC";

	public static CharSequence toCharSequence(Cursor c, int haveCol, int wantCol, int resultCol){
		return toCharSequence(c.getString(haveCol), c.getString(wantCol), c.isNull(resultCol) ? null : c.getDouble(resultCol));
	}
	public static CharSequence toCharSequence(String have, String want, Double result){
		final StringBuilder historyText = new StringBuilder();
		historyText.append(have);
		historyText.append(" = ");
		if (result != null){
			historyText.append(Util.shownumber(result));
			historyText.append(' ');
		}
		historyText.append(want);
		return historyText;
	}
}
