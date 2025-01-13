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
import android.content.SharedPreferences;

public class SharedPrefs {
    private static final String APP_PREFS = "Units";

    public static final String PREF_LAST_CLASSIFICATION_VERSION_CODE = "lastClassificationVersionCode";
    public static final String PREF_LAST_CLASSIFICATION_LOCALE = "lastClassificationLocale";

    public static SharedPreferences getAppPrefs(Context context) {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
    }
}
