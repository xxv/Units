package info.staticfree.android.units;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.CursorAdapter;
import android.widget.TextView;

public class HistoryAdapter extends CursorAdapter {
	public static final String[]
       PROJECTION = {HistoryEntry._ID,
					HistoryEntry._HAVE,
					HistoryEntry._WANT,
					HistoryEntry._RESULT};

	private int have_col;

	private int want_col;

	private int result_col;

	private int time_col;

	public HistoryAdapter(Context context, Cursor c) {
		super(context, c, true);
		if (c != null){
			initColumns(c);
		}
	}

	@Override
	public Cursor swapCursor(Cursor newCursor) {
		initColumns(newCursor);

		return super.swapCursor(newCursor);
	}

	private void initColumns(Cursor c){
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
