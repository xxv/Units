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

import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import net.sourceforge.unitsinjava.Util;

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

    public static CharSequence toCharSequence(Cursor c, int haveCol, int wantCol, int resultCol) {
        return toCharSequence(c.getString(haveCol), c.getString(wantCol), c.isNull(resultCol) ? null : c.getDouble(resultCol));
    }

    public static CharSequence toCharSequence(String have, String want, Double result) {
        final StringBuilder historyText = new StringBuilder();
        historyText.append(have);
        historyText.append(" = ");
        if (result != null) {
            historyText.append(Util.shownumber(result));
            historyText.append(' ');
        }
        historyText.append(want);
        return historyText;
    }
}
