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

        ((TextView) view.findViewById(android.R.id.text1)).setText(HistoryEntry.toCharSequence(cursor, have_col, want_col, result_col));

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
    }
}
