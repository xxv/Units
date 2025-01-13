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

public class UsageEntry implements BaseColumns {
    public static final String
            _UNIT = "unit",
            _USE_COUNT = "usecount",
            _FACTOR_FPRINT = "factors",
            _IS_UNIT_ALIAS = "isunitalias";

    public static final String
            PATH = "units",
            PATH_CONFORM_TOP = PATH + "/by_conform",
            PATH_WITH_CLASSIFICATION = PATH + "/with_classification",
            PATH_NO_UNIT_ALIAS = PATH + "/no_unit_alias",
            PATH_BY_FPRINT = "fprint",
            SORT_DEFAULT = _USE_COUNT + " DESC";

    public final static Uri
            CONTENT_URI = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH),
            CONTENT_URI_CONFORM_TOP = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH_CONFORM_TOP),
            CONTENT_URI_WITH_CLASSIFICATION = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH_WITH_CLASSIFICATION),
            CONTENT_URI_NO_UNIT_ALIAS = Uri.parse("content://" + UnitsContentProvider.AUTHORITY + "/" + PATH_NO_UNIT_ALIAS);

    public static Uri getEntriesMatchingFprint(String fprint) {
        return Uri.withAppendedPath(CONTENT_URI, PATH_BY_FPRINT + "/" + fprint);
    }
}
