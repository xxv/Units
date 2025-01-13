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

import static de.buttercookie.units.SharedPrefs.PREF_LAST_CLASSIFICATION_LOCALE;
import static de.buttercookie.units.SharedPrefs.PREF_LAST_CLASSIFICATION_VERSION_CODE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;

import net.sourceforge.unitsinjava.Env;
import net.sourceforge.unitsinjava.Tables;
import net.sourceforge.unitsinjava.UnitsFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Vector;

public class Application extends android.app.Application {
    private final static String TAG = "Units";
    private final static String UIJ_TAG = "UnitsCore";

    private boolean mUnitsLocaleUpdateRequired = false;

    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            /*StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build());*/
        }

        super.onCreate();
        initUnits();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if (localeChanged(this)) {
            mUnitsLocaleUpdateRequired = true;
        }

        super.onConfigurationChanged(newConfig);
    }

    private void initUnits() {
        Env.filenames = new Vector<>();
        Env.filenames.add("units.dat");

        Env.locale = getCurrentLocale(this);
        Env.quiet = true;
        Env.oneline = true;


        Env.out = new Env.Writer() {
            @Override
            public void print(String s) {
                Log.i(UIJ_TAG, s);

            }

            @Override
            public void println(String s) {
                Log.i(UIJ_TAG, s);

            }
        };

        UnitsFile.fileAcc = new UnitsFile.FileAcc() {

            @Override
            public InputStream open(String name) {
                try {
                    Log.d(TAG, "reading definitions from " + name);
                    return getAssets().open(name);
                } catch (final IOException ioe) {
                    ioe.printStackTrace();
                }
                return null;
            }
        };

        Tables.build();
    }

    public void updateUnitsLocaleIfRequired() {
        if (!mUnitsLocaleUpdateRequired) {
            return;
        }
        mUnitsLocaleUpdateRequired = false;

        Log.d(TAG, "Re-reading units after locale change");
        Env.locale = getCurrentLocale(this);
        Tables.clean();
        Tables.build();
    }

    private static boolean localeChanged(Context context) {
        final SharedPreferences prefs = SharedPrefs.getAppPrefs(context);
        String storedLocale = prefs.getString(PREF_LAST_CLASSIFICATION_LOCALE, null);
        return !getCurrentLocale(context).equals(storedLocale);
    }

    static boolean localeAndVersionUnchanged(Context context) {
        final SharedPreferences prefs = SharedPrefs.getAppPrefs(context);
        String storedLocale = prefs.getString(PREF_LAST_CLASSIFICATION_LOCALE, null);
        int storedVersion = prefs.getInt(PREF_LAST_CLASSIFICATION_VERSION_CODE, 0);

        return getCurrentLocale(context).equals(storedLocale) &&
                BuildConfig.VERSION_CODE == storedVersion;
    }

    @SuppressLint("ApplySharedPref")
    // apply() not available in old SDK
    static void storeLocaleAndVersion(Context context) {
        final SharedPreferences.Editor editor = SharedPrefs.getAppPrefs(context).edit();
        editor.putString(PREF_LAST_CLASSIFICATION_LOCALE, getCurrentLocale(context));
        editor.putInt(PREF_LAST_CLASSIFICATION_VERSION_CODE, BuildConfig.VERSION_CODE);
        editor.commit();
    }

    public static String getCurrentLocale(Context context) {
        final Configuration config = context.getResources().getConfiguration();
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = config.getLocales().get(0);
        } else {
            locale = config.locale;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            locale = locale.stripExtensions();
        }
        return locale.toString();
    }
}
