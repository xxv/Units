package info.staticfree.android.units;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class HistoryAdapter extends CursorAdapter {
	public static final String[]
       PROJECTION = {HistoryEntry._ID,
					HistoryEntry._HAVE,
					HistoryEntry._WANT,
					HistoryEntry._RESULT};

	private final int have_col, want_col, result_col, time_col;

	public HistoryAdapter(Context context, Cursor c) {
		super(context, c, true);
		have_col = c.getColumnIndex(HistoryEntry._HAVE);
		want_col = c.getColumnIndex(HistoryEntry._WANT);
		result_col = c.getColumnIndex(HistoryEntry._RESULT);
		time_col = c.getColumnIndex(HistoryEntry._WHEN);

	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		((TextView)view.findViewById(android.R.id.text1)).setText(HistoryEntry.toCharSequence(cursor, have_col, want_col, result_col));

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		return inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
	}
}
