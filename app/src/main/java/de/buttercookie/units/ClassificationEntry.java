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

import android.net.Uri;
import android.provider.BaseColumns;

public class ClassificationEntry implements BaseColumns {
    public final static String
            _DESCRIPTION = "description",
            _FACTOR_FPRINT = "factors";

    public final static String
            PATH = "classification",
            PATH_BY_FPRINT = "fprint";

    public final static String[]
            PROJECTION = {_ID, _DESCRIPTION, _FACTOR_FPRINT};


    public final static Uri
            CONTENT_URI = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH);

    public static Uri getFprintUri(String fprint) {
        return Uri.withAppendedPath(CONTENT_URI, PATH_BY_FPRINT + "/" + fprint);
    }
}
