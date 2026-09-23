package info.staticfree.android.units;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Vector;

import net.sourceforge.unitsinjava.Env;
import net.sourceforge.unitsinjava.Tables;
import android.util.Log;

public class Application extends android.app.Application {
	private final static String TAG = "units";
	public final static boolean DEBUG = false;

	@Override
	@SuppressWarnings("all")
	public void onCreate() {
//		  if (DEBUG && Build.VERSION.SDK_INT >= 9) {
//		         StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//		                 .detectDiskReads()
//		                 .detectDiskWrites()
//		                 .detectNetwork()   // or .detectAll() for all detectable problems
//		                 .penaltyLog()
//		                 .build());
//		         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//		                 .detectLeakedSqlLiteObjects()
//		                 .penaltyLog()
//		                 .penaltyDeath()
//		                 .build());
//		     }

		super.onCreate();
		initUnits();
	}

    private void initUnits(){
        Env.filenames = new Vector<String>();
        Env.filenames.add("units.dat");

        Env.locale = Locale.getDefault().toString();
        Env.quiet = true;
        Env.oneline = true;


        Env.out = new Env.Writer(){
        	@Override
        	public void print(String s) {
        		Log.i(TAG, s);

        	}

        	@Override
        	public void println(String s) {
        		Log.i(TAG, s);

        	}
        };

        Env.err = new Env.Writer() {

			@Override
			public void println(String s) {
				Log.e(TAG, s);

			}

			@Override
			public void print(String s) {
				Log.e(TAG, s);

			}
		};

        Env.files = new Env.FileAcc() {

			@Override
			public BufferedReader open(String name) {
				try {
					Log.d("Units", "reading definitions from "+name);
					final InputStream is = getAssets().open(name);


					return new BufferedReader(new InputStreamReader(is, "8859_1"), 16000);
				}catch (final IOException ioe){
					ioe.printStackTrace();
				}
				return null;
			}
		};

        Tables.build();
    }
}
